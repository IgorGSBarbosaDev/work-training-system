package dev.igorbarbosa.worktrainingsystem.qualifications.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityOperationsCatalog;
import dev.igorbarbosa.worktrainingsystem.activities.application.QualificationRecalculationPort;
import dev.igorbarbosa.worktrainingsystem.activities.application.QualificationRecalculationRequested;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentStatusPort;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog.EmployeeSummary;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
import dev.igorbarbosa.worktrainingsystem.organizations.application.QualificationSettingsCatalog;
import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse.BlockingReason;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.ActivityQualification;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationBlockingType;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import dev.igorbarbosa.worktrainingsystem.qualifications.persistence.ActivityQualificationRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.application.SliceBNotificationPort;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualificationService implements QualificationCommandPort, QualificationRecalculationPort {
	private static final int CHUNK_SIZE = 100;
	private final ActivityQualificationRepository qualifications;
	private final ActivityOperationsCatalog activities;
	private final EmployeeActivityCatalog employees;
	private final TrainingCatalog trainings;
	private final TrainingCompliancePort compliance;
	private final AssignmentStatusPort assignmentStatuses;
	private final QualificationSettingsCatalog settings;
	private final AuthorizationService authorization;
	private final ObjectMapper json;
	private final Clock clock;
	private final SliceBNotificationPort notifications;

	@Autowired
	public QualificationService(ActivityQualificationRepository qualifications, ActivityOperationsCatalog activities,
			EmployeeActivityCatalog employees, TrainingCatalog trainings, TrainingCompliancePort compliance,
			AssignmentStatusPort assignmentStatuses, QualificationSettingsCatalog settings,
			AuthorizationService authorization, ObjectMapper json, Clock clock, SliceBNotificationPort notifications) {
		this.qualifications = qualifications; this.activities = activities; this.employees = employees;
		this.trainings = trainings; this.compliance = compliance; this.assignmentStatuses = assignmentStatuses;
		this.settings = settings; this.authorization = authorization; this.json = json; this.clock = clock;
		this.notifications = notifications;
	}

	/** Compatibility constructor for the Phase 4 unit-test boundary. */
	public QualificationService(ActivityQualificationRepository qualifications, ActivityOperationsCatalog activities,
			EmployeeActivityCatalog employees, TrainingCatalog trainings, TrainingCompliancePort compliance,
			AssignmentStatusPort assignmentStatuses, QualificationSettingsCatalog settings,
			AuthorizationService authorization, ObjectMapper json, Clock clock) {
		this(qualifications, activities, employees, trainings, compliance, assignmentStatuses, settings,
				authorization, json, clock, new SliceBNotificationPort() {
				@Override public void expirationChanged(ExpirationNotification event) { }
				@Override public void qualificationBlocked(QualificationBlockedNotification event) { }
			});
	}

	@Transactional
	public QualificationResponse calculate(UUID employeeId, UUID activityId) {
		EmployeeSummary employee = employees.requireEmployee(employeeId);
		var activity = activities.requireActivity(activityId);
		boolean assigned = activity.active() && activities.isEffectivelyAssigned(employeeId, activityId);
		Instant now = clock.instant(); LocalDate today = LocalDate.now(clock);
		List<BlockingReason> reasons = new ArrayList<>(); LocalDate nextExpiration = null;
		if (assigned) {
			for (var requirement : activities.activeRequirements(activityId)) {
				var training = trainings.summary(requirement.trainingId());
				UUID requiredVersion = requirement.versionPolicy() == RequirementVersionPolicy.FIXED_VERSION
						? trainings.historicalVersion(requirement.trainingId(), requirement.trainingVersionId()).id()
						: trainings.latestPublishedForCompliance(requirement.trainingId()).id();
				var status = compliance.status(employeeId, requirement.trainingId());
				var completion = status.completion().orElse(null);
				AssignmentStatus assignmentStatus = assignmentStatuses.effectiveStatus(employeeId,
						requirement.trainingId(), requiredVersion).orElse(null);
				if (status.unresolvedFailedAssessment()) {
					reasons.add(reason(QualificationBlockingType.FAILED_ASSESSMENT, requirement.id(), training,
							requiredVersion, completion, assignmentStatus));
					continue;
				}
				if (completion == null || !completion.trainingVersionId().equals(requiredVersion)) {
					reasons.add(reason(QualificationBlockingType.MISSING_TRAINING, requirement.id(), training,
							requiredVersion, completion, assignmentStatus));
					continue;
				}
				if (completion.expirationDate() != null && completion.expirationDate().isBefore(today)) {
					reasons.add(reason(QualificationBlockingType.EXPIRED_TRAINING, requirement.id(), training,
							requiredVersion, completion, assignmentStatus));
					continue;
				}
				if (completion.expirationDate() != null && (nextExpiration == null
						|| completion.expirationDate().isBefore(nextExpiration))) nextExpiration = completion.expirationDate();
			}
		}
		QualificationStatus status;
		if (!assigned) status = QualificationStatus.NOT_ASSIGNED;
		else if (!reasons.isEmpty()) status = QualificationStatus.BLOCKED;
		else if (nextExpiration != null && !nextExpiration.isAfter(today.plusDays(settings.expiringSoonDays(DEFAULT_ORGANIZATION_ID))))
			status = QualificationStatus.EXPIRING;
		else status = QualificationStatus.AVAILABLE;
		String reasonsJson = write(reasons);
		qualifications.upsert(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID, employeeId, activityId, status.name(),
				now, nextExpiration, reasonsJson);
		if (status == QualificationStatus.BLOCKED)
			notifications.qualificationBlocked(new SliceBNotificationPort.QualificationBlockedNotification(
					DEFAULT_ORGANIZATION_ID, employeeId, activityId));
		ActivityQualification persisted = qualifications.findByOrganizationIdAndEmployeeIdAndActivityId(
				DEFAULT_ORGANIZATION_ID, employeeId, activityId).orElseThrow();
		return response(persisted, employee, activity, reasons);
	}

	@Transactional(readOnly = true)
	public Page<QualificationResponse> list(UUID employeeId, UUID activityId, QualificationStatus status, Pageable pageable) {
		Specification<ActivityQualification> specification = visible();
		if (employeeId != null) specification = specification.and(equal("employeeId", employeeId));
		if (activityId != null) specification = specification.and(equal("activityId", activityId));
		if (status != null) specification = specification.and(equal("status", status));
		Page<ActivityQualification> page = qualifications.findAll(specification, pageable);
		Map<UUID, EmployeeSummary> employeeMap = employees.summaries(page.getContent().stream().map(ActivityQualification::getEmployeeId).toList());
		var activityMap = activities.summaries(page.getContent().stream().map(ActivityQualification::getActivityId).toList());
		return page.map(item -> response(item, employeeMap.get(item.getEmployeeId()), activityMap.get(item.getActivityId()), read(item.getBlockingReasons())));
	}

	@Transactional(readOnly = true)
	public QualificationResponse get(UUID id) {
		ActivityQualification item = qualifications.findOne(visible().and(equal("id", id)))
				.orElseThrow(() -> inaccessibleOrMissing(id));
		return response(item, employees.requireEmployee(item.getEmployeeId()),
				activities.requireActivity(item.getActivityId()), read(item.getBlockingReasons()));
	}

	@Transactional
	public QualificationResponse getEmployeeActivity(UUID employeeId, UUID activityId) {
		if (!authorization.canAccessEmployee(employeeId)) throw new AccessDeniedException("Colaborador fora do escopo autorizado.");
		return calculate(employeeId, activityId);
	}

	@Override @Transactional
	public int recalculateEmployee(UUID employeeId) {
		EmployeeSummary employee = employees.requireEmployee(employeeId);
		if (!employee.active()) return 0;
		int count = 0;
		for (var activity : activities.activeForEmployee(employeeId)) { calculate(employeeId, activity.id()); count++; }
		return count;
	}

	@Override @Transactional
	public int recalculateActivity(UUID activityId) {
		activities.requireActivity(activityId); int count = 0; int page = 0; Page<UUID> employeeIds;
		do {
			employeeIds = activities.activeEmployeeIds(activityId, PageRequest.of(page++, CHUNK_SIZE, Sort.by("employeeId")));
			for (UUID employeeId : employeeIds) { calculate(employeeId, activityId); count++; }
		} while (employeeIds.hasNext());
		return count;
	}

	@Override
	public void recalculate(QualificationRecalculationRequested event) {
		if (!DEFAULT_ORGANIZATION_ID.equals(event.organizationId())) return;
		for (UUID employeeId : event.employeeIds()) calculate(employeeId, event.activityId());
	}

	private BlockingReason reason(QualificationBlockingType type, UUID requirementId,
			TrainingCatalog.TrainingSummary training, UUID requiredVersion,
			TrainingCompliancePort.CompletionEvidence completion, AssignmentStatus assignmentStatus) {
		return new BlockingReason(type, requirementId, training.id(), training.name(), requiredVersion,
				completion == null ? null : completion.trainingVersionId(),
				completion == null ? null : completion.expirationDate(), assignmentStatus);
	}
	private QualificationResponse response(ActivityQualification item, EmployeeSummary employee,
			ActivityOperationsCatalog.ActivitySummary activity, List<BlockingReason> reasons) {
		return new QualificationResponse(item.getId(), new QualificationResponse.Employee(employee.id(), employee.name(), employee.registration()),
				new QualificationResponse.Activity(activity.id(), activity.name()), item.getStatus(), item.getCalculatedAt(),
				item.getNextExpirationDate(), List.copyOf(reasons), QualificationResponse.DISCLAIMER);
	}
	private Specification<ActivityQualification> visible() {
		AccessScope scope = authorization.currentScope(); Specification<ActivityQualification> result = organization();
		if (scope.admin()) return result;
		if (scope.employee()) return result.and(equal("employeeId", scope.ownEmployeeId()));
		if (!scope.manager() || !scope.hasGrants()) return result.and((root, query, cb) -> cb.disjunction());
		Set<UUID> employeeIds = authorization.scopeReferences(scope).employeeIds();
		return employeeIds.isEmpty() ? result.and((root, query, cb) -> cb.disjunction())
				: result.and((root, query, cb) -> root.get("employeeId").in(employeeIds));
	}
	private Specification<ActivityQualification> organization() {
		return (root, query, cb) -> cb.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
	}
	private Specification<ActivityQualification> equal(String property, Object value) {
		return (root, query, cb) -> cb.equal(root.get(property), value);
	}
	private RuntimeException inaccessibleOrMissing(UUID id) {
		return qualifications.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent()
				? new AccessDeniedException("A qualificação está fora do escopo autorizado.")
				: new ResourceNotFoundException("A qualificação informada não existe.");
	}
	private String write(List<BlockingReason> reasons) {
		try { return json.writeValueAsString(reasons); }
		catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize qualification reasons", exception); }
	}
	private List<BlockingReason> read(String value) {
		try { return json.readValue(value, new TypeReference<>() {}); }
		catch (JsonProcessingException exception) { throw new IllegalStateException("Could not read qualification reasons", exception); }
	}
}
