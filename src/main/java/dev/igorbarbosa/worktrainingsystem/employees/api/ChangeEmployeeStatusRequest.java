package dev.igorbarbosa.worktrainingsystem.employees.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEmployeeStatusRequest(
		@NotNull(message = "O status é obrigatório.") RegistrationStatus status) {
}
