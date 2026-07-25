package dev.igorbarbosa.worktrainingsystem.assignments.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BatchAssignmentRequest(
		@NotNull UUID trainingId,
		UUID trainingVersionId,
		@NotNull @Valid Target target,
		@FutureOrPresent LocalDate dueDate,
		@NotNull AssignmentPriority priority,
		boolean skipEmployeesWithValidCompletion,
		boolean skipExistingActiveAssignments,
		@Size(max = 200) String idempotencyKey) {
	public record Target(@NotNull AssignmentOrigin type, UUID employeeId, UUID jobId, UUID activityId,
			UUID sectorId, UUID unitId, @Size(max = 500) List<UUID> employeeIds) {
		public Target { employeeIds = employeeIds == null ? List.of() : List.copyOf(employeeIds); }
	}
}
