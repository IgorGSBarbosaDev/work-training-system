package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VideoRequest(
		@NotBlank(message = "O título é obrigatório.")
		@Size(max = 150, message = "O título deve possuir no máximo 150 caracteres.")
		String title,
		@Size(max = 2000, message = "A descrição deve possuir no máximo 2000 caracteres.")
		String description,
		@Positive(message = "A ordem deve ser positiva.")
		int order,
		@Positive(message = "A duração deve ser positiva.")
		int durationSeconds,
		@NotBlank(message = "A referência do arquivo é obrigatória.")
		@Size(max = 2048, message = "A referência do arquivo deve possuir no máximo 2048 caracteres.")
		String storageObjectKey,
		boolean required,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
