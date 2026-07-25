package dev.igorbarbosa.worktrainingsystem.activities.application;

import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import java.util.Set;
import java.util.UUID;

public record ActivityAssignmentRequested(UUID organizationId, Set<UUID> employeeIds, UUID activityId,
		UUID requirementId, UUID trainingId, RequirementVersionPolicy versionPolicy, UUID trainingVersionId,
		UUID responsibleUserId) {}
