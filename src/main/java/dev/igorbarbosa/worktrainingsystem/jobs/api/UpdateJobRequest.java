package dev.igorbarbosa.worktrainingsystem.jobs.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateJobRequest(
		@Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.") String name,
		@Size(max = 1000, message = "A descrição deve possuir no máximo 1000 caracteres.") String description) {
	public boolean hasChanges() { return name != null || description != null; }
}
