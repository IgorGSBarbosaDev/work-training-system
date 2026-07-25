package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
		@Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.") String name,
		RegistrationStatus status) {
	public boolean hasChanges() { return name != null || status != null; }
}
