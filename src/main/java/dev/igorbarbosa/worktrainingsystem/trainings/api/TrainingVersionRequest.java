package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TrainingVersionRequest(
		@Positive(message = "A carga horária deve ser positiva.")
		int workloadMinutes,
		@NotNull(message = "O tipo de validade é obrigatório.")
		ValidityType validityType,
		@Positive(message = "O valor da validade deve ser positivo.")
		Integer validityValue,
		@NotNull(message = "A nota mínima é obrigatória.")
		@DecimalMin(value = "70.0")
		@DecimalMax(value = "100.0")
		BigDecimal passingScore,
		@Positive(message = "O máximo de tentativas deve ser positivo.")
		Integer maxAttempts,
		@Min(value = 0, message = "O intervalo entre tentativas não pode ser negativo.")
		int retryIntervalMinutes) {
}
