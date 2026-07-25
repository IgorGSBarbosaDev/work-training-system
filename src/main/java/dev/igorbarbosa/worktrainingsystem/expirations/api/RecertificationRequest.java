package dev.igorbarbosa.worktrainingsystem.expirations.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record RecertificationRequest(@NotNull UUID completionId, LocalDate dueDate, AssignmentPriority priority) {}
