package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeRegistrationStatusRequest(@NotNull(message = "O status é obrigatório.") RegistrationStatus status) {}
