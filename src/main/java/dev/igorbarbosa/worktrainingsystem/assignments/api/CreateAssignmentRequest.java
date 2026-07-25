package dev.igorbarbosa.worktrainingsystem.assignments.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAssignmentRequest(
		@NotNull UUID employeeId,
		@NotNull UUID trainingId,
		UUID trainingVersionId,
		@NotNull AssignmentOrigin origin,
		UUID sourceReferenceId,
		@FutureOrPresent LocalDate dueDate,
		@NotNull AssignmentPriority priority,
		@Size(max = 200) String idempotencyKey) {
}
