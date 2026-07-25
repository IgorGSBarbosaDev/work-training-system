package dev.igorbarbosa.worktrainingsystem.activities.application;

import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public activity boundary used by assignments and qualifications. */
public interface ActivityOperationsCatalog {
	ActivitySummary requireActivity(UUID activityId);
	boolean isEffectivelyAssigned(UUID employeeId, UUID activityId);
	List<ActivitySummary> activeForEmployee(UUID employeeId);
	Page<UUID> activeEmployeeIds(UUID activityId, Pageable pageable);
	Page<UUID> activeEmployeeIds(UUID activityId, Collection<UUID> allowedEmployeeIds, Pageable pageable);
	List<RequirementSummary> activeRequirements(UUID activityId);
	Map<UUID, ActivitySummary> summaries(Collection<UUID> activityIds);

	record ActivitySummary(UUID id, String name, boolean active) {}
	record RequirementSummary(UUID id, UUID activityId, UUID trainingId,
			RequirementVersionPolicy versionPolicy, UUID trainingVersionId) {}
}
