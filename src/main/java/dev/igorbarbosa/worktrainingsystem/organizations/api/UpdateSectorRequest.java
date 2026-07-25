package dev.igorbarbosa.worktrainingsystem.organizations.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateSectorRequest(
		UUID unitId,
		@Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.") String name,
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{0,19}$",
				message = "O código deve possuir até 20 letras, números, hífens ou sublinhados.") String code) {
	public boolean hasChanges() { return unitId != null || name != null || code != null; }
}
