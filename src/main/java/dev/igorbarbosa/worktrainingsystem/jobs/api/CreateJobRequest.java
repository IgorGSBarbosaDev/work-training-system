package dev.igorbarbosa.worktrainingsystem.jobs.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
		String name,
		@Size(max = 1000, message = "A descrição deve possuir no máximo 1000 caracteres.")
		String description,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
