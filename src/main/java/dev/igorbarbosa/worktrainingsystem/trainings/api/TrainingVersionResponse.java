package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrainingVersionResponse(
		UUID id,
		UUID trainingId,
		int versionNumber,
		int workloadMinutes,
		ValidityType validityType,
		Integer validityValue,
		BigDecimal passingScore,
		Integer maxAttempts,
		int retryIntervalMinutes,
		TrainingVersionStatus status,
		Instant publishedAt) {

	public static TrainingVersionResponse from(TrainingVersion version) {
		return new TrainingVersionResponse(version.getId(), version.getTrainingId(), version.getVersionNumber(),
				version.getWorkloadMinutes(), version.getValidityType(), version.getValidityValue(), version.getPassingScore(),
				version.getMaxAttempts(), version.getRetryIntervalMinutes(), version.getStatus(), version.getPublishedAt());
	}
}
