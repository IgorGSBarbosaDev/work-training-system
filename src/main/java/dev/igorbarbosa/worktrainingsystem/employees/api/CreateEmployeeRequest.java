package dev.igorbarbosa.worktrainingsystem.employees.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateEmployeeRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
		String name,
		@NotBlank(message = "A matrícula é obrigatória.")
		@Size(max = 50, message = "A matrícula deve possuir no máximo 50 caracteres.")
		String registration,
		@NotBlank(message = "O e-mail é obrigatório.")
		@Email(message = "O e-mail deve ser válido.")
		@Size(max = 254, message = "O e-mail deve possuir no máximo 254 caracteres.")
		String email,
		@NotNull(message = "O cargo é obrigatório.")
		UUID jobId,
		@NotNull(message = "O setor é obrigatório.")
		UUID sectorId,
		@NotNull(message = "A unidade é obrigatória.")
		UUID unitId,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
