package dev.igorbarbosa.worktrainingsystem.employees.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateEmployeeRequest(
		@Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
		String name,
		@Pattern(regexp = ".*\\S.*", message = "A matrícula não pode ser vazia.")
		@Size(max = 50, message = "A matrícula deve possuir no máximo 50 caracteres.")
		String registration,
		@Pattern(regexp = ".*\\S.*", message = "O e-mail não pode ser vazio.")
		@Email(message = "O e-mail deve ser válido.")
		@Size(max = 254, message = "O e-mail deve possuir no máximo 254 caracteres.")
		String email,
		UUID sectorId,
		UUID unitId) {

	public boolean hasChanges() {
		return name != null || registration != null || email != null || sectorId != null || unitId != null;
	}
}
