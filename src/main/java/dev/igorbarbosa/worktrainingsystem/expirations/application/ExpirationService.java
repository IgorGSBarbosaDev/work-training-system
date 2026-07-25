package dev.igorbarbosa.worktrainingsystem.expirations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.expirations.api.ExpirationRecalculationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.api.ExpirationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.api.RecertificationRequest;
import dev.igorbarbosa.worktrainingsystem.expirations.api.RecertificationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.CompletionExpirationState;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.CompletionExpirationStatusHistory;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.ExpirationStatus;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.Recertification;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.RecertificationTrigger;
import dev.igorbarbosa.worktrainingsystem.expirations.persistence.CompletionExpirationStateRepository;
import dev.igorbarbosa.worktrainingsystem.expirations.persistence.CompletionExpirationStatusHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.expirations.persistence.ExpirationQueryRepository;
import dev.igorbarbosa.worktrainingsystem.expirations.persistence.RecertificationRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuditPort;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.notifications.application.SliceBNotificationPort;
import dev.igorbarbosa.worktrainingsystem.organizations.application.QualificationSettingsCatalog;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpirationService {
	private static final UUID EMPTY_SCOPE = new UUID(0, 0);
	private final ExpirationQueryRepository queries;
	private final TrainingCompletionRepository completions;
	private final CompletionExpirationStateRepository states;
	private final CompletionExpirationStatusHistoryRepository history;
	private final RecertificationRepository recertifications;
	private final TrainingAssignmentRepository assignments;
	private final EmployeeActivityCatalog employees;
	private final TrainingCatalog trainings;
	private final QualificationSettingsCatalog settings;
	private final QualificationCommandPort qualifications;
	private final SliceBNotificationPort notifications;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final AuditPort audit;
	private final Clock clock;

	public ExpirationService(ExpirationQueryRepository queries, TrainingCompletionRepository completions,
			CompletionExpirationStateRepository states, CompletionExpirationStatusHistoryRepository history,
			RecertificationRepository recertifications, TrainingAssignmentRepository assignments,
			EmployeeActivityCatalog employees, TrainingCatalog trainings, QualificationSettingsCatalog settings,
			QualificationCommandPort qualifications, SliceBNotificationPort notifications,
			AuthorizationService authorization, CurrentUserProvider currentUser, AuditPort audit, Clock clock) {
		this.queries = queries; this.completions = completions; this.states = states; this.history = history;
		this.recertifications = recertifications; this.assignments = assignments; this.employees = employees;
		this.trainings = trainings; this.settings = settings; this.qualifications = qualifications;
		this.notifications = notifications; this.authorization = authorization; this.currentUser = currentUser;
		this.audit = audit; this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<ExpirationResponse> list(UUID employeeId, UUID trainingId, UUID unitId, UUID sectorId, UUID jobId,
			ExpirationStatus status, LocalDate expiresFrom, LocalDate expiresTo, Pageable pageable) {
		if (expiresFrom != null && expiresTo != null && expiresFrom.isAfter(expiresTo))
			throw new BusinessRuleViolationException("INVALID_DATE_RANGE", "O período de vencimento é inválido.");
		var scope = authorization.currentScope();
		Set<UUID> allowed = scope.admin() ? Set.of(EMPTY_SCOPE) : authorization.scopeReferences(scope).employeeIds();
		if (!scope.admin() && (!scope.manager() || allowed.isEmpty())) allowed = Set.of(EMPTY_SCOPE);
		LocalDate today = LocalDate.now(clock);
		LocalDate windowEnd = today.plusDays(settings.expiringSoonDays(DEFAULT_ORGANIZATION_ID));
		return queries.findExpirations(DEFAULT_ORGANIZATION_ID, today, windowEnd, employeeId, trainingId,
				unitId, sectorId, jobId, status == null ? null : status.name(), expiresFrom, expiresTo,
				scope.admin(), allowed, pageable).map(value -> new ExpirationResponse(value.getCompletionId(),
						value.getEmployeeId(), value.getTrainingId(), value.getCompletionDate(),
						value.getExpirationDate(), ExpirationStatus.valueOf(value.getStatus())));
	}

	@Transactional
	public ExpirationRecalculationResponse recalculate() {
		LocalDate today = LocalDate.now(clock);
		LocalDate windowEnd = today.plusDays(settings.expiringSoonDays(DEFAULT_ORGANIZATION_ID));
		int evaluated = 0; int created = 0;
		for (var candidate : queries.findCandidates(DEFAULT_ORGANIZATION_ID, windowEnd)) {
			evaluated++;
			ExpirationStatus status = candidate.getExpirationDate().isBefore(today)
					? ExpirationStatus.EXPIRED : ExpirationStatus.EXPIRING_SOON;
			updateState(candidate.getCompletionId(), candidate.getEmployeeId(), candidate.getTrainingId(),
					candidate.getExpirationDate(), status);
			if (!candidate.getExpirationDate().isAfter(today)
					&& createRecertification(candidate.getCompletionId(), null, AssignmentPriority.NORMAL,
							RecertificationTrigger.AUTOMATIC) != null) created++;
			if (status == ExpirationStatus.EXPIRED) qualifications.recalculateEmployee(candidate.getEmployeeId());
		}
		return new ExpirationRecalculationResponse(evaluated, created);
	}

	@Transactional
	public RecertificationResponse create(RecertificationRequest request) {
		if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now(clock)))
			throw new BusinessRuleViolationException("DUE_DATE_IN_PAST", "O prazo não pode estar no passado.");
		Recertification value = createRecertification(request.completionId(), request.dueDate(),
				request.priority() == null ? AssignmentPriority.NORMAL : request.priority(), RecertificationTrigger.MANUAL);
		if (value == null) return recertifications.findByOrganizationIdAndCompletionId(DEFAULT_ORGANIZATION_ID,
				request.completionId()).map(this::response).orElseThrow(() -> new ResourceConflictException(
						"ACTIVE_RELATION_ALREADY_EXISTS", "Já existe uma atribuição ativa para este treinamento."));
		return response(value);
	}

	@Transactional(readOnly = true)
	public Page<RecertificationResponse> list(Pageable pageable) {
		var scope = authorization.currentScope();
		Specification<Recertification> specification = (root, query, cb) -> cb.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
		if (!scope.admin()) {
			Set<UUID> employeeIds = scope.manager() ? authorization.scopeReferences(scope).employeeIds() : Set.of();
			specification = specification.and((root, query, cb) -> {
				var subquery = query.subquery(UUID.class);
				var completion = subquery.from(TrainingCompletion.class);
				subquery.select(completion.get("id")).where(completion.get("employeeId").in(employeeIds));
				return root.get("completionId").in(subquery);
			});
		}
		return recertifications.findAll(specification, pageable).map(this::response);
	}

	private void updateState(UUID completionId, UUID employeeId, UUID trainingId, LocalDate expiration,
			ExpirationStatus status) {
		Instant now = clock.instant();
		CompletionExpirationState state = states.findByCompletionIdAndOrganizationId(completionId,
				DEFAULT_ORGANIZATION_ID).orElse(null);
		ExpirationStatus previous = state == null ? null : state.getStatus();
		if (state == null) states.save(new CompletionExpirationState(completionId, DEFAULT_ORGANIZATION_ID, status, now));
		else state.evaluate(status, now);
		if (previous != status) {
			history.save(new CompletionExpirationStatusHistory(DEFAULT_ORGANIZATION_ID, completionId, previous,
					status, expiration, now));
			notifications.expirationChanged(new SliceBNotificationPort.ExpirationNotification(
					DEFAULT_ORGANIZATION_ID, employeeId, trainingId, completionId, expiration, status.name()));
		}
	}

	private Recertification createRecertification(UUID completionId, LocalDate dueDate, AssignmentPriority priority,
			RecertificationTrigger trigger) {
		var existing = recertifications.findByOrganizationIdAndCompletionId(DEFAULT_ORGANIZATION_ID, completionId);
		if (existing.isPresent()) return null;
		TrainingCompletion completion = completions.findByIdAndOrganizationId(completionId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A conclusão informada não existe."));
		var employee = employees.requireEmployee(completion.getEmployeeId());
		if (!employee.active()) return null;
		if (trigger == RecertificationTrigger.MANUAL && !authorization.canAccessEmployee(employee.id()))
			throw new AccessDeniedException("Colaborador fora do escopo autorizado.");
		UUID actor = trigger == RecertificationTrigger.MANUAL ? currentUser.requireCurrentUser().userId()
				: responsibleFor(completion);
		var version = trainings.resolveLatestPublished(completion.getTrainingId());
		Instant now = clock.instant(); UUID assignmentId = UUID.randomUUID();
		int inserted = assignments.insertRecertificationIfAbsent(assignmentId, DEFAULT_ORGANIZATION_ID,
				completion.getEmployeeId(), completion.getTrainingId(), version.id(), now, LocalDate.now(clock), dueDate,
				priority.name(), actor, completionId);
		if (inserted == 0) return null;
		Recertification saved = recertifications.saveAndFlush(new Recertification(DEFAULT_ORGANIZATION_ID,
				completionId, assignmentId, trigger, actor, now));
		qualifications.recalculateEmployee(completion.getEmployeeId());
		audit.record(new AuditPort.AuditRecord(actor, "RECERTIFICATION_CREATED", "RECERTIFICATION", saved.getId(),
				now, Map.of("trigger", trigger.name(), "completionId", completionId.toString())));
		return saved;
	}

	private UUID responsibleFor(TrainingCompletion completion) {
		if (completion.getResponsibleUserId() != null) return completion.getResponsibleUserId();
		if (completion.getSourceAssignmentId() != null) return assignments.findByIdAndOrganizationId(
				completion.getSourceAssignmentId(), DEFAULT_ORGANIZATION_ID).orElseThrow().getResponsibleUserId();
		throw new IllegalStateException("Completion has no responsible user");
	}

	private RecertificationResponse response(Recertification value) {
		return new RecertificationResponse(value.getId(), value.getCompletionId(), value.getAssignmentId(),
				value.getTriggerType(), value.getResponsibleUserId(), value.getCreatedAt());
	}
}
