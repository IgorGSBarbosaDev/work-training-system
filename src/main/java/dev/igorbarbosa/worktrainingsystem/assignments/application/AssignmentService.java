package dev.igorbarbosa.worktrainingsystem.assignments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityAssignmentRequested;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityOperationsCatalog;
import dev.igorbarbosa.worktrainingsystem.activities.application.AssignmentGenerationPort;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentBatchResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentReasonRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.api.BatchAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.CreateAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.UpdateAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatch;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchResult;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchResultType;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentBatchRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentBatchResultRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentSourceRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog.EmployeeSummary;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.TrainingCompliancePort;
import dev.igorbarbosa.worktrainingsystem.notifications.application.SliceBNotificationPort;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService implements AssignmentGenerationPort {
	private static final EnumSet<AssignmentStatus> EFFECTIVE = EnumSet.of(AssignmentStatus.NOT_STARTED,
			AssignmentStatus.IN_PROGRESS, AssignmentStatus.AWAITING_ASSESSMENT,
			AssignmentStatus.APPROVED, AssignmentStatus.FAILED);
	private static final int CHUNK_SIZE = 100;

	private final TrainingAssignmentRepository assignments;
	private final AssignmentSourceRepository sources;
	private final AssignmentBatchRepository batches;
	private final AssignmentBatchResultRepository batchResults;
	private final EmployeeActivityCatalog employees;
	private final ActivityOperationsCatalog activities;
	private final TrainingCatalog trainings;
	private final TrainingCompliancePort compliance;
	private final QualificationCommandPort qualifications;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final SliceBNotificationPort notifications;

	@Autowired
	public AssignmentService(TrainingAssignmentRepository assignments, AssignmentSourceRepository sources,
			AssignmentBatchRepository batches, AssignmentBatchResultRepository batchResults,
			EmployeeActivityCatalog employees, ActivityOperationsCatalog activities, TrainingCatalog trainings,
			TrainingCompliancePort compliance, QualificationCommandPort qualifications,
			AuthorizationService authorization, CurrentUserProvider currentUser, SliceBNotificationPort notifications) {
		this.assignments = assignments; this.sources = sources; this.batches = batches;
		this.batchResults = batchResults; this.employees = employees; this.activities = activities;
		this.trainings = trainings; this.compliance = compliance; this.qualifications = qualifications;
		this.authorization = authorization; this.currentUser = currentUser; this.notifications = notifications;
	}

	/** Compatibility constructor for Docker-independent assignment unit tests. */
	public AssignmentService(TrainingAssignmentRepository assignments, AssignmentSourceRepository sources,
			AssignmentBatchRepository batches, AssignmentBatchResultRepository batchResults,
			EmployeeActivityCatalog employees, ActivityOperationsCatalog activities, TrainingCatalog trainings,
			TrainingCompliancePort compliance, QualificationCommandPort qualifications,
			AuthorizationService authorization, CurrentUserProvider currentUser) {
		this(assignments, sources, batches, batchResults, employees, activities, trainings, compliance, qualifications,
				authorization, currentUser, new SliceBNotificationPort() {
				@Override public void expirationChanged(ExpirationNotification event) { }
				@Override public void qualificationBlocked(QualificationBlockedNotification event) { }
			});
	}

	@Transactional
	public AssignmentResponse create(CreateAssignmentRequest request, String headerKey) {
		CurrentUser actor = currentUser.requireCurrentUser();
		validateDueDate(request.dueDate());
		requireEmployeeScope(request.employeeId());
		if (request.origin() == AssignmentOrigin.RECERTIFICATION)
			throw rule("INVALID_ASSIGNMENT_ORIGIN", "Use a operação de reciclagem para essa origem.");
		EmployeeSummary employee = employees.requireActiveEmployee(request.employeeId());
		var version = resolveVersion(request.trainingId(), request.trainingVersionId());
		UUID sourceId = sourceReference(request.origin(), request.sourceReferenceId(), employee);
		String key = key(headerKey, request.idempotencyKey());
		String hash = hash(request.employeeId(), request.trainingId(), version.id(), request.origin(), sourceId,
				request.dueDate(), request.priority());
		if (key != null) {
			var previous = assignments.findByOrganizationIdAndResponsibleUserIdAndIdempotencyKey(
					DEFAULT_ORGANIZATION_ID, actor.userId(), key);
			if (previous.isPresent()) return response(requireSameHash(previous.get(), hash));
		}
		CreateOutcome outcome = createEffective(employee, request.trainingId(), version.id(), request.origin(), sourceId,
				request.dueDate(), request.priority(), actor.userId(), null, null, key, hash);
		if (!outcome.created()) throw duplicate();
		qualifications.recalculateEmployee(employee.id());
		return response(outcome.assignment());
	}

	@Transactional
	public AssignmentBatchResponse createBatch(BatchAssignmentRequest request, String headerKey) {
		CurrentUser actor = currentUser.requireCurrentUser();
		validateDueDate(request.dueDate());
		validateTarget(request.target());
		var version = resolveVersion(request.trainingId(), request.trainingVersionId());
		String key = key(headerKey, request.idempotencyKey());
		String hash = hash(request.trainingId(), version.id(), request.target(), request.dueDate(), request.priority(),
				request.skipEmployeesWithValidCompletion(), request.skipExistingActiveAssignments());
		if (key != null) {
			var previous = batches.findByOrganizationIdAndRequestedByUserIdAndIdempotencyKey(
					DEFAULT_ORGANIZATION_ID, actor.userId(), key);
			if (previous.isPresent()) {
				if (!previous.get().getRequestHash().equals(hash)) throw idempotencyConflict();
				return batchResponse(previous.get());
			}
		}
		AssignmentBatch batch = batches.saveAndFlush(new AssignmentBatch(DEFAULT_ORGANIZATION_ID, actor.userId(), key, hash));
		int[] counts = new int[4];
		Set<UUID> allowedEmployeeIds = batchAllowedEmployeeIds();
		forEachSelectedEmployee(request.target(), allowedEmployeeIds, (selectedId, employee) -> {
			counts[0]++;
			if (employee == null) {
				batchResults.save(failed(batch, selectedId, "RESOURCE_NOT_FOUND", "Colaborador não encontrado."));
				counts[3]++; return;
			}
			if (!authorization.canAccessEmployee(employee.id())) {
				batchResults.save(failed(batch, employee.id(), "ACCESS_DENIED", "Colaborador fora do escopo autorizado."));
				counts[3]++; return;
			}
			if (!employee.active()) {
				batchResults.save(failed(batch, employee.id(), "EMPLOYEE_INACTIVE", "Colaborador inativo."));
				counts[3]++; return;
			}
			if (request.skipEmployeesWithValidCompletion() && hasValidCompletion(employee.id(), request.trainingId(), version.id())) {
				batchResults.save(skipped(batch, employee.id(), "VALID_COMPLETION", "Conclusão válida já registrada."));
				counts[2]++; return;
			}
			UUID sourceId = batchSource(request.target(), employee, batch.getId());
			CreateOutcome outcome = createEffective(employee, request.trainingId(), version.id(), request.target().type(),
					sourceId, request.dueDate(), request.priority(), actor.userId(), null, batch.getId(), null, null);
			if (outcome.created()) {
				batchResults.save(new AssignmentBatchResult(DEFAULT_ORGANIZATION_ID, batch.getId(), employee.id(),
						AssignmentBatchResultType.CREATED, outcome.assignment().getId(), null, null)); counts[1]++;
				qualifications.recalculateEmployee(employee.id());
			} else if (request.skipExistingActiveAssignments()) {
				batchResults.save(skipped(batch, employee.id(), "ACTIVE_ASSIGNMENT_EXISTS", "Atribuição ativa já existente.")); counts[2]++;
			} else {
				batchResults.save(failed(batch, employee.id(), "ACTIVE_RELATION_ALREADY_EXISTS", "Atribuição ativa já existente.")); counts[3]++;
			}
		});
		batch.complete(counts[0], counts[1], counts[2], counts[3]);
		batches.flush(); batchResults.flush();
		return batchResponse(batch);
	}

	@Transactional(readOnly = true)
	public Page<AssignmentResponse> list(UUID employeeId, UUID trainingId, AssignmentStatus status,
			AssignmentOrigin origin, LocalDate dueFrom, LocalDate dueTo, Pageable pageable) {
		Specification<TrainingAssignment> specification = visible();
		if (employeeId != null) specification = specification.and(equal("employeeId", employeeId));
		if (trainingId != null) specification = specification.and(equal("trainingId", trainingId));
		if (status != null) specification = specification.and(equal("status", status));
		if (origin != null) specification = specification.and(equal("origin", origin));
		if (dueFrom != null) specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
		if (dueTo != null) specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
		if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) throw rule("INVALID_DATE_RANGE", "Período de prazo inválido.");
		Page<TrainingAssignment> page = assignments.findAll(specification, pageable);
		Map<UUID, EmployeeSummary> employeeMap = employees.summaries(page.getContent().stream().map(TrainingAssignment::getEmployeeId).toList());
		var trainingMap = trainings.summaries(page.getContent().stream().map(TrainingAssignment::getTrainingId).toList());
		return page.map(item -> response(item, employeeMap.get(item.getEmployeeId()), trainingMap.get(item.getTrainingId())));
	}

	@Transactional(readOnly = true)
	public AssignmentResponse get(UUID id) { return response(findVisible(id)); }

	@Transactional(readOnly = true)
	public Page<AssignmentResponse> mine(AssignmentStatus status, Pageable pageable) {
		AccessScope scope = authorization.currentScope();
		if (!scope.employee() || scope.ownEmployeeId() == null) throw new AccessDeniedException("Usuário sem colaborador vinculado.");
		Specification<TrainingAssignment> specification = organization().and(equal("employeeId", scope.ownEmployeeId()));
		if (status != null) specification = specification.and(equal("status", status));
		Page<TrainingAssignment> page = assignments.findAll(specification, pageable);
		return page.map(this::response);
	}

	@Transactional(readOnly = true)
	public AssignmentResponse mine(UUID id) {
		AccessScope scope = authorization.currentScope();
		TrainingAssignment item = assignments.findOne(organization().and(equal("id", id))
				.and(equal("employeeId", scope.ownEmployeeId()))).orElseThrow(() -> new ResourceNotFoundException("A atribuição informada não existe."));
		return response(item);
	}

	@Transactional
	public AssignmentResponse update(UUID id, UpdateAssignmentRequest request) {
		if (!request.hasChanges()) throw rule("NO_CHANGES", "Informe prazo ou prioridade.");
		validateDueDate(request.dueDate());
		TrainingAssignment item = findVisible(id);
		try { item.update(request.dueDate() == null ? item.getDueDate() : request.dueDate(),
				request.priority() == null ? item.getPriority() : request.priority()); }
		catch (IllegalStateException exception) { throw invalidTransition(); }
		qualifications.recalculateEmployee(item.getEmployeeId());
		return response(item);
	}

	@Transactional
	public AssignmentResponse cancel(UUID id, AssignmentReasonRequest request) {
		TrainingAssignment item = findVisible(id);
		try { item.cancel(currentUser.requireCurrentUser().userId(), request.reason().trim(), Instant.now()); }
		catch (IllegalStateException exception) { throw invalidTransition(); }
		qualifications.recalculateEmployee(item.getEmployeeId());
		return response(item);
	}

	@Transactional
	public AssignmentResponse waive(UUID id, AssignmentReasonRequest request) {
		TrainingAssignment item = findVisible(id);
		try { item.waive(currentUser.requireCurrentUser().userId(), request.reason().trim(), Instant.now()); }
		catch (IllegalStateException exception) { throw invalidTransition(); }
		qualifications.recalculateEmployee(item.getEmployeeId());
		return response(item);
	}

	@Transactional
	public AssignmentResponse recycle(UUID id, String idempotencyKey) {
		TrainingAssignment previous = findVisible(id);
		if (!previous.getStatus().isTerminal() || previous.getStatus() == AssignmentStatus.CANCELLED
				|| previous.getStatus() == AssignmentStatus.WAIVED)
			throw invalidTransition();
		EmployeeSummary employee = employees.requireActiveEmployee(previous.getEmployeeId());
		var version = trainings.resolveLatestPublished(previous.getTrainingId());
		UUID actor = currentUser.requireCurrentUser().userId();
		String key = key(idempotencyKey, null);
		String hash = hash(previous.getId(), version.id(), AssignmentOrigin.RECERTIFICATION);
		if (key != null) {
			var existing = assignments.findByOrganizationIdAndResponsibleUserIdAndIdempotencyKey(
					DEFAULT_ORGANIZATION_ID, actor, key);
			if (existing.isPresent()) return response(requireSameHash(existing.get(), hash));
		}
		CreateOutcome outcome = createEffective(employee, previous.getTrainingId(), version.id(), AssignmentOrigin.RECERTIFICATION,
				previous.getId(), null, previous.getPriority(), actor, previous.getId(), null, key, hash);
		if (!outcome.created()) throw duplicate();
		qualifications.recalculateEmployee(employee.id());
		return response(outcome.assignment());
	}

	@Transactional(readOnly = true)
	public AssignmentBatchResponse getBatch(UUID id) {
		AssignmentBatch batch = batches.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O lote informado não existe."));
		if (!batch.getRequestedByUserId().equals(currentUser.requireCurrentUser().userId()))
			throw new AccessDeniedException("O lote pertence a outro solicitante.");
		return batchResponse(batch);
	}

	@Override @Transactional
	public int generate(ActivityAssignmentRequested event) {
		if (!DEFAULT_ORGANIZATION_ID.equals(event.organizationId())) return 0;
		UUID versionId = event.versionPolicy() == RequirementVersionPolicy.FIXED_VERSION
				? trainings.requirePublishedVersion(event.trainingId(), event.trainingVersionId()).id()
				: trainings.resolveLatestPublished(event.trainingId()).id();
		int created = 0;
		for (UUID employeeId : event.employeeIds()) {
			EmployeeSummary employee;
			try { employee = employees.requireActiveEmployee(employeeId); }
			catch (BusinessRuleViolationException | ResourceNotFoundException exception) { continue; }
			CreateOutcome result = createEffective(employee, event.trainingId(), versionId, AssignmentOrigin.ACTIVITY,
					event.requirementId(), null, AssignmentPriority.NORMAL, event.responsibleUserId(), null, null, null, null);
			if (result.created()) created++;
		}
		return created;
	}

	private CreateOutcome createEffective(EmployeeSummary employee, UUID trainingId, UUID versionId,
			AssignmentOrigin origin, UUID sourceId, LocalDate dueDate, AssignmentPriority priority, UUID actor,
			UUID recertificationOf, UUID batchId, String idempotencyKey, String requestHash) {
		Instant now = Instant.now(); UUID id = UUID.randomUUID();
		int inserted = assignments.insertIfAbsent(id, DEFAULT_ORGANIZATION_ID, employee.id(), trainingId, versionId,
				origin.name(), now, now.atZone(java.time.ZoneOffset.UTC).toLocalDate(), dueDate, priority.name(), actor,
				origin == AssignmentOrigin.RECERTIFICATION, recertificationOf, idempotencyKey, requestHash, batchId);
		TrainingAssignment item;
		if (inserted == 1) item = assignments.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).orElseThrow();
		else if (idempotencyKey != null) item = assignments.findByOrganizationIdAndResponsibleUserIdAndIdempotencyKey(
				DEFAULT_ORGANIZATION_ID, actor, idempotencyKey).map(value -> requireSameHash(value, requestHash)).orElseGet(
						() -> existingEffective(employee.id(), trainingId, versionId));
		else item = existingEffective(employee.id(), trainingId, versionId);
		sources.insertIfAbsent(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID, item.getId(), origin.name(), sourceId, now);
		if (inserted == 1) notifications.assignmentCreated(new SliceBNotificationPort.AssignmentNotification(
				DEFAULT_ORGANIZATION_ID, employee.id(), item.getId(), trainingId));
		return new CreateOutcome(item, inserted == 1);
	}

	private TrainingAssignment existingEffective(UUID employeeId, UUID trainingId, UUID versionId) {
		return assignments.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdAndTrainingVersionIdAndStatusIn(
				DEFAULT_ORGANIZATION_ID, employeeId, trainingId, versionId, EFFECTIVE)
				.orElseThrow(() -> new ResourceConflictException("CONCURRENT_ASSIGNMENT_CONFLICT", "Outra requisição criou a atribuição."));
	}

	private void forEachSelectedEmployee(BatchAssignmentRequest.Target target, Set<UUID> allowedEmployeeIds,
			java.util.function.BiConsumer<UUID, EmployeeSummary> consumer) {
		if (target.type() == AssignmentOrigin.EMPLOYEE) {
			acceptEmployee(target.employeeId(), consumer); return;
		}
		if (target.type() == AssignmentOrigin.GROUP) {
			LinkedHashSet<UUID> unique = new LinkedHashSet<>(target.employeeIds());
			for (UUID id : unique) acceptEmployee(id, consumer);
			return;
		}
		int page = 0;
		if (target.type() == AssignmentOrigin.ACTIVITY) {
			Page<UUID> ids;
			do {
				ids = activities.activeEmployeeIds(target.activityId(), allowedEmployeeIds,
						PageRequest.of(page++, CHUNK_SIZE, Sort.by("employeeId")));
				Map<UUID, EmployeeSummary> summaries = employees.summaries(ids.getContent());
				for (UUID id : ids) consumer.accept(id, summaries.get(id));
			} while (ids.hasNext());
			return;
		}
		Page<EmployeeSummary> values;
		do {
			Pageable pageable = PageRequest.of(page++, CHUNK_SIZE, Sort.by("id"));
			values = switch (target.type()) {
				case JOB -> employees.findActiveByJob(target.jobId(), allowedEmployeeIds, pageable);
				case SECTOR -> employees.findActiveBySector(target.sectorId(), allowedEmployeeIds, pageable);
				case UNIT -> employees.findActiveByUnit(target.unitId(), allowedEmployeeIds, pageable);
				default -> Page.empty(pageable);
			};
			values.forEach(employee -> consumer.accept(employee.id(), employee));
		} while (values.hasNext());
	}

	private void acceptEmployee(UUID employeeId, java.util.function.BiConsumer<UUID, EmployeeSummary> consumer) {
		try { consumer.accept(employeeId, employees.requireEmployee(employeeId)); }
		catch (ResourceNotFoundException exception) { consumer.accept(employeeId, null); }
	}

	private Set<UUID> batchAllowedEmployeeIds() {
		AccessScope scope = authorization.currentScope();
		if (scope.admin()) return null;
		if (!scope.manager() || !scope.hasGrants()) return Set.of();
		return authorization.scopeReferences(scope).employeeIds();
	}

	private void validateTarget(BatchAssignmentRequest.Target target) {
		if (target.type() == AssignmentOrigin.RECERTIFICATION) throw rule("INVALID_ASSIGNMENT_ORIGIN", "Reciclagem não é destino de lote.");
		boolean valid = switch (target.type()) {
			case EMPLOYEE -> target.employeeId() != null;
			case JOB -> target.jobId() != null;
			case ACTIVITY -> target.activityId() != null;
			case SECTOR -> target.sectorId() != null;
			case UNIT -> target.unitId() != null;
			case GROUP -> !target.employeeIds().isEmpty() && target.employeeIds().size() <= 500;
			default -> false;
		};
		if (!valid) throw rule("INVALID_ASSIGNMENT_TARGET", "O identificador do destino é obrigatório.");
	}

	private UUID sourceReference(AssignmentOrigin origin, UUID requested, EmployeeSummary employee) {
		return switch (origin) {
			case EMPLOYEE -> employee.id();
			case JOB -> employee.jobId();
			case SECTOR -> employee.sectorId();
			case UNIT -> employee.unitId();
			case ACTIVITY, GROUP -> {
				if (requested == null) throw rule("SOURCE_REFERENCE_REQUIRED", "A referência da origem é obrigatória.");
				yield requested;
			}
			case RECERTIFICATION -> throw rule("INVALID_ASSIGNMENT_ORIGIN", "Origem inválida.");
		};
	}

	private UUID batchSource(BatchAssignmentRequest.Target target, EmployeeSummary employee, UUID batchId) {
		return switch (target.type()) {
			case EMPLOYEE -> employee.id(); case JOB -> target.jobId(); case ACTIVITY -> target.activityId();
			case SECTOR -> target.sectorId(); case UNIT -> target.unitId(); case GROUP -> batchId;
			default -> throw rule("INVALID_ASSIGNMENT_ORIGIN", "Origem inválida.");
		};
	}

	private boolean hasValidCompletion(UUID employeeId, UUID trainingId, UUID versionId) {
		return compliance.status(employeeId, trainingId).completion()
				.filter(item -> item.trainingVersionId().equals(versionId))
				.filter(item -> item.expirationDate() == null || !item.expirationDate().isBefore(LocalDate.now(java.time.Clock.systemUTC())))
				.isPresent();
	}

	private TrainingCatalog.VersionSummary resolveVersion(UUID trainingId, UUID versionId) {
		trainings.requireActiveTraining(trainingId);
		return versionId == null ? trainings.resolveLatestPublished(trainingId)
				: trainings.requirePublishedVersion(trainingId, versionId);
	}

	private TrainingAssignment findVisible(UUID id) {
		return assignments.findOne(visible().and(equal("id", id))).orElseThrow(() -> {
			if (assignments.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent())
				return new AccessDeniedException("A atribuição está fora do escopo autorizado.");
			return new ResourceNotFoundException("A atribuição informada não existe.");
		});
	}

	private void requireEmployeeScope(UUID employeeId) {
		if (!authorization.canAccessEmployee(employeeId)) throw new AccessDeniedException("Colaborador fora do escopo autorizado.");
	}
	private Specification<TrainingAssignment> visible() {
		AccessScope scope = authorization.currentScope(); Specification<TrainingAssignment> result = organization();
		if (scope.admin()) return result;
		if (scope.employee()) return result.and(equal("employeeId", scope.ownEmployeeId()));
		if (!scope.manager() || !scope.hasGrants()) return result.and((root, query, cb) -> cb.disjunction());
		Set<UUID> ids = authorization.scopeReferences(scope).employeeIds();
		return ids.isEmpty() ? result.and((root, query, cb) -> cb.disjunction())
				: result.and((root, query, cb) -> root.get("employeeId").in(ids));
	}
	private Specification<TrainingAssignment> organization() {
		return (root, query, cb) -> cb.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
	}
	private Specification<TrainingAssignment> equal(String property, Object value) {
		return (root, query, cb) -> cb.equal(root.get(property), value);
	}

	private AssignmentResponse response(TrainingAssignment item) {
		return response(item, employees.requireEmployee(item.getEmployeeId()), trainings.summary(item.getTrainingId()));
	}
	private AssignmentResponse response(TrainingAssignment item, EmployeeSummary employee, TrainingCatalog.TrainingSummary training) {
		int number = trainings.historicalVersion(item.getTrainingId(), item.getTrainingVersionId()).versionNumber();
		List<AssignmentResponse.Source> provenance = sources.findAllByOrganizationIdAndAssignmentIdOrderByCreatedAt(
				DEFAULT_ORGANIZATION_ID, item.getId()).stream()
				.map(source -> new AssignmentResponse.Source(source.getOrigin(), source.getSourceReferenceId())).toList();
		return new AssignmentResponse(item.getId(), new AssignmentResponse.Reference(employee.id(), employee.name()),
				new AssignmentResponse.Reference(training.id(), training.name()), item.getTrainingVersionId(), number,
				item.getOrigin(), provenance, item.getAssignedAt(), item.getAssignedDate(), item.getDueDate(), item.getStatus(),
				item.getPriority(), item.getResponsibleUserId(), item.isRecertification(), item.getRecertificationOfAssignmentId(),
				item.getCancelledAt(), item.getCancellationReason(), item.getWaivedAt(), item.getWaiverReason(), item.getBatchId(),
				item.getCreatedAt(), item.getUpdatedAt());
	}
	private AssignmentBatchResponse batchResponse(AssignmentBatch batch) {
		List<AssignmentBatchResponse.Result> results = batchResults.findAllByOrganizationIdAndBatchIdOrderByEmployeeId(
				DEFAULT_ORGANIZATION_ID, batch.getId()).stream().map(item -> new AssignmentBatchResponse.Result(
						item.getEmployeeId(), item.getResult(), item.getAssignmentId(), item.getCode(), item.getMessage())).toList();
		return new AssignmentBatchResponse(batch.getId(), batch.getStatus(), batch.getRequestedCount(),
				batch.getCreatedCount(), batch.getSkippedCount(), batch.getFailedCount(), results);
	}
	private AssignmentBatchResult failed(AssignmentBatch batch, UUID employeeId, String code, String message) {
		return new AssignmentBatchResult(DEFAULT_ORGANIZATION_ID, batch.getId(), employeeId,
				AssignmentBatchResultType.FAILED, null, code, message);
	}
	private AssignmentBatchResult skipped(AssignmentBatch batch, UUID employeeId, String code, String message) {
		return new AssignmentBatchResult(DEFAULT_ORGANIZATION_ID, batch.getId(), employeeId,
				AssignmentBatchResultType.SKIPPED, null, code, message);
	}
	private TrainingAssignment requireSameHash(TrainingAssignment item, String hash) {
		if (!hash.equals(item.getRequestHash())) throw idempotencyConflict(); return item;
	}
	private String key(String header, String body) {
		String value = header != null && !header.isBlank() ? header : body;
		if (value == null || value.isBlank()) return null;
		value = value.trim();
		if (value.length() > 200) throw rule("IDEMPOTENCY_KEY_INVALID", "A chave de idempotência excede 200 caracteres.");
		return value;
	}
	private String hash(Object... values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(java.util.Arrays.deepToString(values).getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(bytes);
		} catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
	}
	private void validateDueDate(LocalDate dueDate) {
		if (dueDate != null && dueDate.isBefore(LocalDate.now(java.time.Clock.systemUTC())))
			throw rule("INVALID_DUE_DATE", "O prazo não pode ser anterior à data da atribuição.");
	}
	private ResourceConflictException duplicate() { return new ResourceConflictException("ACTIVE_RELATION_ALREADY_EXISTS", "Já existe atribuição ativa para a versão."); }
	private ResourceConflictException idempotencyConflict() { return new ResourceConflictException("IDEMPOTENCY_KEY_REUSED", "A chave já foi usada com outro conteúdo."); }
	private ResourceConflictException invalidTransition() { return new ResourceConflictException("INVALID_STATE_TRANSITION", "A transição de estado não é permitida."); }
	private BusinessRuleViolationException rule(String code, String message) { return new BusinessRuleViolationException(code, message); }
	private record CreateOutcome(TrainingAssignment assignment, boolean created) {}
}
