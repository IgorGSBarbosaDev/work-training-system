package dev.igorbarbosa.worktrainingsystem.activities.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.activities.api.ActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.ActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.EmployeeActivityDetailResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.EmployeeActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.JobActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.JobActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.ManualEmployeeActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.RelatedJobResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.RequirementRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.RequirementResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.UpdateActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.UpdateRequirementRequest;
import dev.igorbarbosa.worktrainingsystem.activities.domain.Activity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.ActivityTrainingRequirement;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import dev.igorbarbosa.worktrainingsystem.activities.domain.JobActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityTrainingRequirementRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.EmployeeActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.JobActivityRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
	private static final int PROPAGATION_CHUNK_SIZE = 100;
	private final ActivityRepository activities;
	private final JobActivityRepository jobActivities;
	private final EmployeeActivityRepository employeeActivities;
	private final ActivityTrainingRequirementRepository requirements;
	private final EmployeeActivityCatalog employees;
	private final JobActivityCatalog jobs;
	private final TrainingCatalog trainings;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final ApplicationEventPublisher events;

	public ActivityService(ActivityRepository activities, JobActivityRepository jobActivities,
			EmployeeActivityRepository employeeActivities, ActivityTrainingRequirementRepository requirements,
			EmployeeActivityCatalog employees, JobActivityCatalog jobs, TrainingCatalog trainings,
			AuthorizationService authorization, CurrentUserProvider currentUser, ApplicationEventPublisher events) {
		this.activities = activities; this.jobActivities = jobActivities; this.employeeActivities = employeeActivities;
		this.requirements = requirements; this.employees = employees; this.jobs = jobs; this.trainings = trainings;
		this.authorization = authorization; this.currentUser = currentUser; this.events = events;
	}

	@Transactional
	public ActivityResponse create(ActivityRequest request) {
		String name = request.name().trim();
		if (activities.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)) throw activityConflict();
		try {
			return ActivityResponse.from(activities.saveAndFlush(new Activity(DEFAULT_ORGANIZATION_ID, name,
					trim(request.description()), request.status())));
		} catch (DataIntegrityViolationException exception) { throw activityConflict(); }
	}

	@Transactional(readOnly = true)
	public Page<ActivityResponse> list(String search, RegistrationStatus status, LocalDate createdFrom,
			LocalDate createdTo, Pageable pageable) {
		Specification<Activity> specification = visibleActivities();
		String normalized = normalize(search);
		if (normalized != null) specification = specification.and((root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("name")), "%" + normalized + "%"),
				cb.like(cb.lower(root.get("description")), "%" + normalized + "%")));
		if (status != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
		if (createdFrom != null) {
			Instant from = createdFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
			specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
		}
		if (createdTo != null) {
			Instant toExclusive = createdTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
			specification = specification.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive));
		}
		if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo))
			throw rule("INVALID_DATE_RANGE", "A data inicial não pode ser posterior à data final.");
		return activities.findAll(specification, pageable).map(ActivityResponse::from);
	}

	@Transactional(readOnly = true)
	public ActivityResponse get(UUID activityId) { return ActivityResponse.from(findVisible(activityId)); }

	@Transactional
	public ActivityResponse update(UUID activityId, UpdateActivityRequest request) {
		if (!request.hasChanges()) throw rule("NO_CHANGES", "Informe ao menos um campo para atualização.");
		Activity activity = find(activityId);
		String name = request.name() == null ? activity.getName() : request.name().trim();
		if (!name.equalsIgnoreCase(activity.getName())
				&& activities.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)) throw activityConflict();
		activity.update(name, request.description() == null ? activity.getDescription() : trim(request.description()));
		try { activities.flush(); } catch (DataIntegrityViolationException exception) { throw activityConflict(); }
		return ActivityResponse.from(activity);
	}

	@Transactional
	public ActivityResponse changeStatus(UUID activityId, RegistrationStatus status) {
		Activity activity = find(activityId); activity.changeStatus(status); return ActivityResponse.from(activity);
	}

	@Transactional
	public JobActivityResponse addJobActivity(UUID jobId, JobActivityRequest request) {
		jobs.requireActiveJob(jobId);
		Activity activity = requireActiveActivity(request.activityId());
		var existing = jobActivities.findByOrganizationIdAndJobIdAndActivityIdAndStatus(
				DEFAULT_ORGANIZATION_ID, jobId, activity.getId(), RegistrationStatus.ACTIVE);
		if (existing.isPresent()) return jobResponse(existing.get(), activity, 0);
		UUID actor = actor(); Instant now = Instant.now();
		JobActivity link;
		try { link = jobActivities.saveAndFlush(new JobActivity(DEFAULT_ORGANIZATION_ID, jobId, activity.getId(), actor, now)); }
		catch (DataIntegrityViolationException exception) {
			link = jobActivities.findByOrganizationIdAndJobIdAndActivityIdAndStatus(DEFAULT_ORGANIZATION_ID, jobId,
					activity.getId(), RegistrationStatus.ACTIVE).orElseThrow(() -> exception);
			return jobResponse(link, activity, 0);
		}
		int linked = 0;
		if (request.shouldApplyToCurrentEmployees()) {
			int page = 0; Page<EmployeeActivityCatalog.EmployeeSummary> values;
			do {
				values = employees.findActiveByJob(jobId, null, PageRequest.of(page++, PROPAGATION_CHUNK_SIZE, Sort.by("id")));
				Set<UUID> employeeIds = values.stream().map(EmployeeActivityCatalog.EmployeeSummary::id).collect(Collectors.toSet());
				for (UUID employeeId : employeeIds) if (addEmployeeOrigin(employeeId, activity.getId(), EmployeeActivityOrigin.JOB,
						link.getId(), null, actor, now)) linked++;
				propagate(employeeIds, activity.getId(), true);
			} while (values.hasNext());
		}
		return jobResponse(link, activity, linked);
	}

	@Transactional
	public void removeJobActivity(UUID jobId, UUID activityId) {
		jobs.requireJob(jobId); find(activityId);
		var link = jobActivities.findByOrganizationIdAndJobIdAndActivityIdAndStatus(
				DEFAULT_ORGANIZATION_ID, jobId, activityId, RegistrationStatus.ACTIVE);
		if (link.isEmpty()) return;
		UUID actor = actor(); Instant now = Instant.now();
		Set<UUID> affected = employeeActivities.findAllByOrganizationIdAndSourceJobActivityIdAndStatus(
				DEFAULT_ORGANIZATION_ID, link.get().getId(), RegistrationStatus.ACTIVE).stream()
				.peek(item -> item.deactivate(actor, now)).map(EmployeeActivity::getEmployeeId).collect(Collectors.toSet());
		link.get().deactivate(actor, now);
		events.publishEvent(new QualificationRecalculationRequested(DEFAULT_ORGANIZATION_ID, affected, activityId));
	}

	@Transactional(readOnly = true)
	public List<JobActivityResponse> listJobActivities(UUID jobId) {
		jobs.requireJob(jobId);
		return jobActivities.findAllByOrganizationIdAndJobIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, jobId, RegistrationStatus.ACTIVE).stream()
				.map(link -> jobResponse(link, find(link.getActivityId()), 0)).toList();
	}

	@Transactional(readOnly = true)
	public List<RelatedJobResponse> listActivityJobs(UUID activityId) {
		findVisible(activityId);
		AccessScope scope = authorization.currentScope();
		Set<UUID> visibleJobIds = scope.admin() ? null : authorization.scopeReferences(scope).jobIds();
		return jobActivities.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, activityId, RegistrationStatus.ACTIVE).stream()
				.filter(link -> visibleJobIds == null || visibleJobIds.contains(link.getJobId()))
				.map(link -> { var job = jobs.requireJob(link.getJobId()); return new RelatedJobResponse(job.id(), job.name(), link.getLinkedAt()); })
				.toList();
	}

	@Transactional
	public EmployeeActivityResponse addManualEmployeeActivity(UUID employeeId, ManualEmployeeActivityRequest request) {
		employees.requireActiveEmployee(employeeId); Activity activity = requireActiveActivity(request.activityId());
		UUID actor = actor(); Instant now = Instant.now();
		addEmployeeOrigin(employeeId, activity.getId(), EmployeeActivityOrigin.MANUAL, null, trim(request.reason()), actor, now);
		propagate(Set.of(employeeId), activity.getId(), true);
		return effective(employeeId, activity.getId());
	}

	@Transactional
	public void removeManualEmployeeActivity(UUID employeeId, UUID activityId) {
		employees.requireEmployee(employeeId); find(activityId);
		employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndOriginAndStatus(DEFAULT_ORGANIZATION_ID,
				employeeId, activityId, EmployeeActivityOrigin.MANUAL, RegistrationStatus.ACTIVE)
				.ifPresent(link -> link.deactivate(actor(), Instant.now()));
		events.publishEvent(new QualificationRecalculationRequested(DEFAULT_ORGANIZATION_ID, Set.of(employeeId), activityId));
	}

	@Transactional(readOnly = true)
	public List<EmployeeActivityResponse> listEmployeeActivities(UUID employeeId) {
		requireEmployeeRead(employeeId);
		return effectiveLinks(employeeId).values().stream().map(group -> toEffective(employeeId, group)).toList();
	}

	@Transactional(readOnly = true)
	public EmployeeActivityDetailResponse getEmployeeActivity(UUID employeeId, UUID activityId) {
		requireEmployeeRead(employeeId);
		return new EmployeeActivityDetailResponse(effective(employeeId, activityId), listRequirementsInternal(activityId));
	}

	@Transactional
	public RequirementResponse addRequirement(UUID activityId, RequirementRequest request) {
		Activity activity = requireActiveActivity(activityId); rejectOptional(request.required());
		TrainingCatalog.TrainingSummary training = trainings.requireActiveTraining(request.trainingId());
		UUID versionId = validatePolicy(training.id(), request.versionPolicy(), request.trainingVersionId());
		var existing = requirements.existsByOrganizationIdAndActivityIdAndTrainingIdAndStatus(DEFAULT_ORGANIZATION_ID,
				activityId, training.id(), RegistrationStatus.ACTIVE);
		if (existing) throw new ResourceConflictException("ACTIVE_RELATION_ALREADY_EXISTS", "O requisito ativo já existe.");
		ActivityTrainingRequirement requirement;
		try { requirement = requirements.saveAndFlush(new ActivityTrainingRequirement(DEFAULT_ORGANIZATION_ID, activityId,
				training.id(), request.versionPolicy(), versionId, actor(), Instant.now())); }
		catch (DataIntegrityViolationException exception) { throw activeRelationConflict(); }
		publishRequirementEvents(requirement, request.shouldApplyToCurrentEmployees());
		return RequirementResponse.from(requirement, training);
	}

	@Transactional
	public RequirementResponse updateRequirement(UUID activityId, UUID requirementId, UpdateRequirementRequest request) {
		find(activityId); rejectOptional(request.required());
		ActivityTrainingRequirement requirement = findRequirement(activityId, requirementId);
		UUID versionId = validatePolicy(requirement.getTrainingId(), request.versionPolicy(), request.trainingVersionId());
		requirement.update(request.versionPolicy(), versionId);
		publishRequirementEvents(requirement, request.shouldApplyToCurrentEmployees());
		return RequirementResponse.from(requirement, trainings.summary(requirement.getTrainingId()));
	}

	@Transactional
	public void removeRequirement(UUID activityId, UUID requirementId) {
		ActivityTrainingRequirement requirement = findRequirement(activityId, requirementId);
		if (requirement.getStatus() == RegistrationStatus.INACTIVE) return;
		requirement.deactivate(actor(), Instant.now());
		forEachActiveEmployeeChunk(activityId, employeeIds -> events.publishEvent(
				new QualificationRecalculationRequested(DEFAULT_ORGANIZATION_ID, employeeIds, activityId)));
	}

	@Transactional(readOnly = true)
	public List<RequirementResponse> listRequirements(UUID activityId) { findVisible(activityId); return listRequirementsInternal(activityId); }

	private List<RequirementResponse> listRequirementsInternal(UUID activityId) {
		return requirements.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(DEFAULT_ORGANIZATION_ID,
				activityId, RegistrationStatus.ACTIVE).stream()
				.map(item -> RequirementResponse.from(item, trainings.summary(item.getTrainingId()))).toList();
	}

	private boolean addEmployeeOrigin(UUID employeeId, UUID activityId, EmployeeActivityOrigin origin,
			UUID source, String reason, UUID actor, Instant now) {
		if (employeeLinkExists(employeeId, activityId, origin, source)) return false;
		try {
			employeeActivities.saveAndFlush(new EmployeeActivity(DEFAULT_ORGANIZATION_ID, employeeId, activityId,
					origin, source, reason, actor, now)); return true;
		} catch (DataIntegrityViolationException exception) {
			if (employeeLinkExists(employeeId, activityId, origin, source)) return false;
			throw exception;
		}
	}

	private boolean employeeLinkExists(UUID employeeId, UUID activityId, EmployeeActivityOrigin origin, UUID source) {
		return origin == EmployeeActivityOrigin.JOB
				? employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndSourceJobActivityIdAndStatus(
						DEFAULT_ORGANIZATION_ID, employeeId, activityId, source, RegistrationStatus.ACTIVE).isPresent()
				: employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndOriginAndStatus(
						DEFAULT_ORGANIZATION_ID, employeeId, activityId, origin, RegistrationStatus.ACTIVE).isPresent();
	}

	private void propagate(Set<UUID> employeeIds, UUID activityId, boolean assignments) {
		if (employeeIds.isEmpty()) return;
		if (assignments) for (ActivityTrainingRequirement requirement : requirements
				.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(DEFAULT_ORGANIZATION_ID, activityId, RegistrationStatus.ACTIVE)) {
			events.publishEvent(assignmentEvent(requirement, employeeIds));
		}
		events.publishEvent(new QualificationRecalculationRequested(DEFAULT_ORGANIZATION_ID, employeeIds, activityId));
	}

	private void publishRequirementEvents(ActivityTrainingRequirement requirement, boolean assignments) {
		forEachActiveEmployeeChunk(requirement.getActivityId(), employeeIds -> {
			if (assignments) events.publishEvent(assignmentEvent(requirement, employeeIds));
			events.publishEvent(new QualificationRecalculationRequested(DEFAULT_ORGANIZATION_ID, employeeIds, requirement.getActivityId()));
		});
	}

	private void forEachActiveEmployeeChunk(UUID activityId, java.util.function.Consumer<Set<UUID>> consumer) {
		int page = 0; Page<UUID> values;
		do {
			values = employeeActivities.findActiveEmployeeIds(DEFAULT_ORGANIZATION_ID, activityId,
					PageRequest.of(page++, PROPAGATION_CHUNK_SIZE, Sort.by("employeeId")));
			if (!values.isEmpty()) consumer.accept(Set.copyOf(values.getContent()));
		} while (values.hasNext());
	}

	private ActivityAssignmentRequested assignmentEvent(ActivityTrainingRequirement requirement, Set<UUID> employeeIds) {
		return new ActivityAssignmentRequested(DEFAULT_ORGANIZATION_ID, employeeIds, requirement.getActivityId(),
				requirement.getId(), requirement.getTrainingId(), requirement.getVersionPolicy(), requirement.getTrainingVersionId(), actor());
	}

	private UUID validatePolicy(UUID trainingId, RequirementVersionPolicy policy, UUID versionId) {
		if (policy == RequirementVersionPolicy.FIXED_VERSION) {
			if (versionId == null) throw rule("TRAINING_VERSION_REQUIRED", "A versão é obrigatória para FIXED_VERSION.");
			return trainings.requirePublishedVersion(trainingId, versionId).id();
		}
		if (versionId != null) throw rule("TRAINING_VERSION_NOT_ALLOWED", "LATEST_PUBLISHED não aceita uma versão fixa.");
		trainings.resolveLatestPublished(trainingId);
		return null;
	}

	private void rejectOptional(Boolean required) {
		if (Boolean.FALSE.equals(required)) throw rule("REQUIREMENT_MUST_BE_REQUIRED", "Requisitos de atividade são obrigatórios no MVP.");
	}

	private Map<UUID, List<EmployeeActivity>> effectiveLinks(UUID employeeId) {
		return employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(DEFAULT_ORGANIZATION_ID,
				employeeId, RegistrationStatus.ACTIVE).stream().collect(Collectors.groupingBy(EmployeeActivity::getActivityId,
				LinkedHashMap::new, Collectors.toList()));
	}

	private EmployeeActivityResponse effective(UUID employeeId, UUID activityId) {
		List<EmployeeActivity> links = effectiveLinks(employeeId).get(activityId);
		if (links == null || links.isEmpty()) throw new ResourceNotFoundException("A atividade não está atribuída ao colaborador.");
		return toEffective(employeeId, links);
	}

	private EmployeeActivityResponse toEffective(UUID employeeId, List<EmployeeActivity> links) {
		Activity activity = find(links.getFirst().getActivityId());
		Set<EmployeeActivityOrigin> origins = links.stream().map(EmployeeActivity::getOrigin)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(EmployeeActivityOrigin.class)));
		Instant assignedAt = links.stream().map(EmployeeActivity::getAssignedAt).min(Comparator.naturalOrder()).orElseThrow();
		return new EmployeeActivityResponse(employeeId, ActivityResponse.from(activity), Set.copyOf(origins), assignedAt, true);
	}

	private Specification<Activity> visibleActivities() {
		AccessScope scope = authorization.currentScope();
		Specification<Activity> organization = (root, query, cb) -> cb.equal(root.get("organizationId"), scope.organizationId());
		if (scope.admin()) return organization;
		if (!scope.manager() || !scope.hasGrants()) return organization.and((root, query, cb) -> cb.disjunction());
		var references = authorization.scopeReferences(scope);
		Set<UUID> employeeIds = references.employeeIds();
		if (employeeIds.isEmpty()) return organization.and((root, query, cb) -> cb.disjunction());
		Set<UUID> activityIds = new java.util.HashSet<>(employeeActivities.findActiveActivityIds(scope.organizationId(), employeeIds));
		if (!references.jobIds().isEmpty()) activityIds.addAll(jobActivities.findActiveActivityIds(scope.organizationId(), references.jobIds()));
		return activityIds.isEmpty() ? organization.and((root, query, cb) -> cb.disjunction())
				: organization.and((root, query, cb) -> root.get("id").in(activityIds));
	}

	private Activity findVisible(UUID id) {
		return activities.findOne(visibleActivities().and((root, query, cb) -> cb.equal(root.get("id"), id)))
				.orElseThrow(() -> inaccessibleOrMissing(id));
	}
	private Activity find(UUID id) { return activities.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
			.orElseThrow(() -> new ResourceNotFoundException("A atividade informada não existe.")); }
	private Activity requireActiveActivity(UUID id) {
		Activity activity = find(id);
		if (activity.getStatus() != RegistrationStatus.ACTIVE) throw rule("ACTIVITY_INACTIVE", "A atividade informada está inativa.");
		return activity;
	}
	private ActivityTrainingRequirement findRequirement(UUID activityId, UUID id) {
		return requirements.findByIdAndActivityIdAndOrganizationId(id, activityId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O requisito informado não existe."));
	}
	private void requireEmployeeRead(UUID employeeId) {
		if (!authorization.canAccessEmployee(employeeId)) throw new AccessDeniedException("O colaborador está fora do escopo autorizado.");
		employees.requireEmployee(employeeId);
	}
	private RuntimeException inaccessibleOrMissing(UUID id) {
		return activities.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent()
				? new AccessDeniedException("A atividade está fora do escopo autorizado.")
				: new ResourceNotFoundException("A atividade informada não existe.");
	}
	private JobActivityResponse jobResponse(JobActivity link, Activity activity, int linked) {
		return new JobActivityResponse(link.getId(), link.getJobId(), ActivityResponse.from(activity), link.getLinkedAt(), linked);
	}
	private UUID actor() { return currentUser.requireCurrentUser().userId(); }
	private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT); }
	private ResourceConflictException activityConflict() { return new ResourceConflictException("ACTIVITY_ALREADY_EXISTS", "Já existe uma atividade com o nome informado."); }
	private ResourceConflictException activeRelationConflict() { return new ResourceConflictException("ACTIVE_RELATION_ALREADY_EXISTS", "O vínculo ativo já existe."); }
	private BusinessRuleViolationException rule(String code, String message) { return new BusinessRuleViolationException(code, message); }
}
