package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AnswerOptionRequest(
		@NotBlank(message = "O texto é obrigatório.")
		@Size(max = 1000, message = "O texto deve possuir no máximo 1000 caracteres.")
		String text,
		boolean correct,
		@Positive(message = "A ordem deve ser positiva.")
		int order,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
