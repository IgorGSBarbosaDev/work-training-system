package dev.igorbarbosa.worktrainingsystem.activities.api;

import dev.igorbarbosa.worktrainingsystem.activities.domain.ActivityTrainingRequirement;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog.TrainingSummary;
import java.time.Instant;
import java.util.UUID;

public record RequirementResponse(UUID id, UUID activityId, TrainingSummary training,
		RequirementVersionPolicy versionPolicy, UUID trainingVersionId, boolean required, Instant linkedAt) {
	public static RequirementResponse from(ActivityTrainingRequirement requirement, TrainingSummary training) {
		return new RequirementResponse(requirement.getId(), requirement.getActivityId(), training,
				requirement.getVersionPolicy(), requirement.getTrainingVersionId(), true, requirement.getLinkedAt());
	}
}
