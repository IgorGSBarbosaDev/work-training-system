package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ModuleRequest(
		@NotBlank(message = "O título é obrigatório.")
		@Size(max = 150, message = "O título deve possuir no máximo 150 caracteres.")
		String title,
		@Size(max = 2000, message = "A descrição deve possuir no máximo 2000 caracteres.")
		String description,
		@Positive(message = "A ordem deve ser positiva.")
		int order,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
