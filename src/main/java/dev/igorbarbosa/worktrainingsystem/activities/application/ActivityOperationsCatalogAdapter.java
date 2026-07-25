package dev.igorbarbosa.worktrainingsystem.activities.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.activities.domain.Activity;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityTrainingRequirementRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.EmployeeActivityRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ActivityOperationsCatalogAdapter implements ActivityOperationsCatalog {
	private final ActivityRepository activities;
	private final EmployeeActivityRepository employeeActivities;
	private final ActivityTrainingRequirementRepository requirements;
	ActivityOperationsCatalogAdapter(ActivityRepository activities, EmployeeActivityRepository employeeActivities,
			ActivityTrainingRequirementRepository requirements) {
		this.activities = activities; this.employeeActivities = employeeActivities; this.requirements = requirements;
	}
	@Override @Transactional(readOnly = true)
	public ActivitySummary requireActivity(UUID activityId) { return summary(find(activityId)); }
	@Override @Transactional(readOnly = true)
	public boolean isEffectivelyAssigned(UUID employeeId, UUID activityId) {
		return employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
				DEFAULT_ORGANIZATION_ID, employeeId, RegistrationStatus.ACTIVE).stream()
				.anyMatch(link -> link.getActivityId().equals(activityId));
	}
	@Override @Transactional(readOnly = true)
	public List<ActivitySummary> activeForEmployee(UUID employeeId) {
		return employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
				DEFAULT_ORGANIZATION_ID, employeeId, RegistrationStatus.ACTIVE).stream()
				.map(link -> link.getActivityId()).distinct().map(this::find).map(this::summary).toList();
	}
	@Override @Transactional(readOnly = true)
	public Page<UUID> activeEmployeeIds(UUID activityId, Pageable pageable) {
		find(activityId);
		return employeeActivities.findActiveEmployeeIds(DEFAULT_ORGANIZATION_ID, activityId, pageable);
	}
	@Override @Transactional(readOnly = true)
	public Page<UUID> activeEmployeeIds(UUID activityId, Collection<UUID> allowedEmployeeIds, Pageable pageable) {
		find(activityId);
		if (allowedEmployeeIds == null) return employeeActivities.findActiveEmployeeIds(DEFAULT_ORGANIZATION_ID, activityId, pageable);
		if (allowedEmployeeIds.isEmpty()) return Page.empty(pageable);
		return employeeActivities.findActiveEmployeeIdsInScope(DEFAULT_ORGANIZATION_ID, activityId, allowedEmployeeIds, pageable);
	}
	@Override @Transactional(readOnly = true)
	public List<RequirementSummary> activeRequirements(UUID activityId) {
		find(activityId);
		return requirements.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, activityId, RegistrationStatus.ACTIVE).stream()
				.map(item -> new RequirementSummary(item.getId(), item.getActivityId(), item.getTrainingId(),
						item.getVersionPolicy(), item.getTrainingVersionId())).toList();
	}
	@Override @Transactional(readOnly = true)
	public Map<UUID, ActivitySummary> summaries(Collection<UUID> activityIds) {
		return activities.findAllById(activityIds).stream()
				.filter(item -> item.getOrganizationId().equals(DEFAULT_ORGANIZATION_ID))
				.collect(Collectors.toUnmodifiableMap(Activity::getId, this::summary));
	}
	private Activity find(UUID id) {
		return activities.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A atividade informada não existe."));
	}
	private ActivitySummary summary(Activity item) {
		return new ActivitySummary(item.getId(), item.getName(), item.getStatus() == RegistrationStatus.ACTIVE);
	}
}
