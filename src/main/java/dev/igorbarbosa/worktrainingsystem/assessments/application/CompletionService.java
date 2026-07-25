package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.api.CompletionResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.ManualCompletionRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionExpirationHistory;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionForm;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.CompletionExpirationHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort.ExecutionAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.application.TrainingReadinessPort;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.files.application.UploadedFileCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompletionService implements TrainingReadinessPort {
	private final TrainingCompletionRepository completions;
	private final CompletionExpirationHistoryRepository expirationHistory;
	private final EmployeeActivityCatalog employees;
	private final TrainingCatalog trainings;
	private final UploadedFileCatalog files;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	public CompletionService(TrainingCompletionRepository completions,
			CompletionExpirationHistoryRepository expirationHistory, EmployeeActivityCatalog employees,
			TrainingCatalog trainings, UploadedFileCatalog files, AuthorizationService authorization,
			CurrentUserProvider currentUser, ApplicationEventPublisher events, Clock clock) {
		this.completions = completions; this.expirationHistory = expirationHistory; this.employees = employees;
		this.trainings = trainings; this.files = files; this.authorization = authorization;
		this.currentUser = currentUser; this.events = events; this.clock = clock;
	}

	@Override @Transactional
	public boolean contentReady(ExecutionAssignment assignment) {
		automatic(assignment, null);
		return true;
	}

	@Transactional
	public CompletionResponse automatic(ExecutionAssignment assignment, BigDecimal finalScore) {
		var rules = trainings.completionRules(assignment.trainingId(), assignment.trainingVersionId());
		Instant now = clock.instant(); LocalDate completionDate = LocalDate.ofInstant(now, ZoneOffset.UTC);
		LocalDate expiration = expiration(completionDate, rules.validityType(), rules.validityValue());
		UUID id = UUID.randomUUID();
		int inserted = completions.insertAutomaticIfAbsent(id, assignment.organizationId(), assignment.employeeId(),
				assignment.trainingId(), assignment.trainingVersionId(), assignment.id(), completionDate, now,
				normalizeScore(finalScore), rules.validityType().name(), rules.validityValue(), expiration);
		TrainingCompletion completion = completions.findByOrganizationIdAndSourceAssignmentId(
				assignment.organizationId(), assignment.id()).orElseThrow();
		if (inserted == 1) events.publishEvent(TrainingOutcomeEvent.completed(assignment.employeeId(), assignment.trainingId(), id));
		return response(completion);
	}

	@Transactional
	public CompletionResponse manual(ManualCompletionRequest request) {
		if (request.completedAt().isAfter(clock.instant())) throw rule("COMPLETION_IN_FUTURE", "A conclusão não pode estar no futuro.");
		employees.requireEmployee(request.employeeId());
		trainings.summary(request.trainingId());
		var rules = trainings.completionRules(request.trainingId(), request.trainingVersionId());
		Validity validity = validity(request.validityType(), request.validityValue(), rules.validityType(), rules.validityValue());
		if (request.externalCertificateFileId() != null)
			files.requireExternalCertificate(request.externalCertificateFileId(), request.employeeId());
		LocalDate date = LocalDate.ofInstant(request.completedAt(), ZoneOffset.UTC);
		TrainingCompletion completion = completions.saveAndFlush(new TrainingCompletion(DEFAULT_ORGANIZATION_ID,
				request.employeeId(), request.trainingId(), request.trainingVersionId(), null, request.completedAt(),
				CompletionForm.MANUAL, normalizeScore(request.score()), validity.type(), validity.value(),
				expiration(date, validity.type(), validity.value()), currentUser.requireCurrentUser().userId(),
				trim(request.notes()), request.externalCertificateFileId()));
		events.publishEvent(TrainingOutcomeEvent.completed(request.employeeId(), request.trainingId(), completion.getId()));
		return response(completion);
	}

	@Transactional(readOnly = true)
	public Page<CompletionResponse> list(UUID employeeId, UUID trainingId, Pageable pageable) {
		Specification<TrainingCompletion> specification = visible();
		if (employeeId != null) specification = specification.and(equal("employeeId", employeeId));
		if (trainingId != null) specification = specification.and(equal("trainingId", trainingId));
		return completions.findAll(specification, pageable).map(this::response);
	}

	@Transactional(readOnly = true)
	public CompletionResponse get(UUID id) {
		TrainingCompletion completion = completions.findOne(visible().and(equal("id", id)))
				.orElseThrow(() -> inaccessibleOrMissing(id));
		return response(completion);
	}

	@Transactional(readOnly = true)
	public Instant automaticCompletedAt(UUID assignmentId) {
		return completions.findByOrganizationIdAndSourceAssignmentId(DEFAULT_ORGANIZATION_ID, assignmentId)
				.map(TrainingCompletion::getCompletedAt).orElse(null);
	}

	@Transactional
	public CompletionResponse recalculateExpiration(UUID id) {
		TrainingCompletion completion = completions.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A conclusão informada não existe."));
		var latest = expirationHistory.findFirstByOrganizationIdAndCompletionIdOrderByCreatedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, id).orElse(null);
		ValidityType type = latest == null ? completion.getAppliedValidityType() : latest.getValidityType();
		Integer value = latest == null ? completion.getAppliedValidityValue() : latest.getValidityValue();
		LocalDate previous = latest == null ? completion.getExpirationDate() : latest.getRecalculatedExpirationDate();
		LocalDate recalculated = expiration(completion.getCompletionDate(), type, value);
		expirationHistory.save(new CompletionExpirationHistory(DEFAULT_ORGANIZATION_ID, id, previous, recalculated,
				type, value, currentUser.requireCurrentUser().userId(), "ADMINISTRATIVE_RECALCULATION", clock.instant()));
		return response(completion, type, value, recalculated);
	}

	public static LocalDate expiration(LocalDate completionDate, ValidityType type, Integer value) {
		if (type == ValidityType.INDEFINITE) return null;
		if (value == null || value <= 0) throw new IllegalArgumentException("validity value");
		return type == ValidityType.DAYS ? completionDate.plusDays(value) : completionDate.plusMonths(value);
	}

	private Validity validity(ValidityType requestedType, Integer requestedValue, ValidityType fallbackType, Integer fallbackValue) {
		ValidityType type = requestedType == null ? fallbackType : requestedType;
		Integer value = requestedType == null ? fallbackValue : requestedValue;
		if (type == ValidityType.INDEFINITE && value != null || type != ValidityType.INDEFINITE && (value == null || value <= 0))
			throw rule("INVALID_VALIDITY", "A validade informada é inválida.");
		return new Validity(type, value);
	}
	private CompletionResponse response(TrainingCompletion item) {
		var latest = expirationHistory.findFirstByOrganizationIdAndCompletionIdOrderByCreatedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, item.getId()).orElse(null);
		return latest == null ? response(item, item.getAppliedValidityType(), item.getAppliedValidityValue(), item.getExpirationDate())
				: response(item, latest.getValidityType(), latest.getValidityValue(), latest.getRecalculatedExpirationDate());
	}
	private CompletionResponse response(TrainingCompletion item, ValidityType type, Integer value, LocalDate expiration) {
		return new CompletionResponse(item.getId(), item.getEmployeeId(), item.getTrainingId(), item.getTrainingVersionId(),
				item.getSourceAssignmentId(), item.getCompletionDate(), item.getCompletedAt(), item.getCompletionForm(),
				item.getFinalScore(), type, value, expiration, item.getResponsibleUserId(), item.getNotes(), item.getExternalEvidenceFileId());
	}
	private Specification<TrainingCompletion> visible() {
		var scope = authorization.currentScope(); Specification<TrainingCompletion> result = equal("organizationId", DEFAULT_ORGANIZATION_ID);
		if (scope.admin()) return result;
		if (scope.employee()) return result.and(equal("employeeId", scope.ownEmployeeId()));
		if (!scope.manager() || !scope.hasGrants()) return result.and((root, query, cb) -> cb.disjunction());
		Set<UUID> employeeIds = authorization.scopeReferences(scope).employeeIds();
		return employeeIds.isEmpty() ? result.and((root, query, cb) -> cb.disjunction())
				: result.and((root, query, cb) -> root.get("employeeId").in(employeeIds));
	}
	private Specification<TrainingCompletion> equal(String property, Object value) {
		return (root, query, cb) -> cb.equal(root.get(property), value);
	}
	private RuntimeException inaccessibleOrMissing(UUID id) {
		return completions.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent()
				? new AccessDeniedException("A conclusão está fora do escopo autorizado.")
				: new ResourceNotFoundException("A conclusão informada não existe.");
	}
	private BigDecimal normalizeScore(BigDecimal score) {
		if (score == null) return null;
		if (score.signum() < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0)
			throw rule("INVALID_SCORE", "A nota deve estar entre zero e cem.");
		return score.setScale(2, RoundingMode.HALF_UP);
	}
	private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private BusinessRuleViolationException rule(String code, String message) { return new BusinessRuleViolationException(code, message); }
	private record Validity(ValidityType type, Integer value) {}
}
