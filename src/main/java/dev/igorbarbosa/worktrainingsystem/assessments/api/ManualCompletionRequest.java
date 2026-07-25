package dev.igorbarbosa.worktrainingsystem.assessments.api;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ManualCompletionRequest(@NotNull UUID employeeId, @NotNull UUID trainingId,
		@NotNull UUID trainingVersionId, @NotNull Instant completedAt,
		@DecimalMin("0.00") @DecimalMax("100.00") BigDecimal score,
		ValidityType validityType, Integer validityValue, @Size(max = 2000) String notes,
		UUID externalCertificateFileId) {}
