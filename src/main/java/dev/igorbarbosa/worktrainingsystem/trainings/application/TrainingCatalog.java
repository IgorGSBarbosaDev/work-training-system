package dev.igorbarbosa.worktrainingsystem.trainings.application;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Collection;
import java.util.Map;

/** Public catalog boundary for modules that need training references. */
public interface TrainingCatalog {
	TrainingSummary requireActiveTraining(UUID trainingId);
	VersionSummary requirePublishedVersion(UUID trainingId, UUID versionId);
	VersionSummary resolveLatestPublished(UUID trainingId);
	VersionSummary historicalVersion(UUID trainingId, UUID versionId);
	VersionSummary latestPublishedForCompliance(UUID trainingId);
	TrainingSummary summary(UUID trainingId);
	Map<UUID, TrainingSummary> summaries(Collection<UUID> trainingIds);
	CompletionRules completionRules(UUID trainingId, UUID versionId);

	record TrainingSummary(UUID id, String name, String code, String description, String category,
			boolean regulatoryStandard, RegistrationStatus status) {}
	record VersionSummary(UUID id, UUID trainingId, int versionNumber, TrainingVersionStatus status) {}
	record CompletionRules(UUID trainingId, UUID versionId, int versionNumber, TrainingVersionStatus status,
			ValidityType validityType, Integer validityValue, BigDecimal passingScore,
			Integer maxAttempts, int retryIntervalMinutes) {}
}
