package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSectorRequest(
		@NotNull(message = "A unidade é obrigatória.")
		UUID unitId,
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
		String name,
		@Pattern(
				regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{0,19}$",
				message = "O código deve possuir até 20 letras, números, hífens ou sublinhados.")
		String code,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status) {
}
