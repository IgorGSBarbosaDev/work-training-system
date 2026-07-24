package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTrainingStatusRequest(@NotNull RegistrationStatus status) {
}
