package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record QuestionnaireRequest(
		@NotBlank(message = "O título é obrigatório.")
		@Size(max = 150, message = "O título deve possuir no máximo 150 caracteres.")
		String title,
		@NotNull(message = "A nota mínima é obrigatória.")
		@DecimalMin(value = "70.0")
		@DecimalMax(value = "100.0")
		BigDecimal passingScore,
		@Positive(message = "O máximo de tentativas deve ser positivo.")
		Integer maxAttempts,
		@jakarta.validation.constraints.Min(value = 0)
		int retryIntervalMinutes,
		boolean shuffleQuestions,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
