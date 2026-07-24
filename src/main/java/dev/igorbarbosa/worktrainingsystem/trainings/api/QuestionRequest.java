package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
		@NotBlank(message = "O enunciado é obrigatório.")
		@Size(max = 2000, message = "O enunciado deve possuir no máximo 2000 caracteres.")
		String statement,
		@Positive(message = "A ordem deve ser positiva.")
		int order,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
