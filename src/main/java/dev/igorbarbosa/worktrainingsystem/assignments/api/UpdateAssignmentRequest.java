package dev.igorbarbosa.worktrainingsystem.assignments.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;

public record UpdateAssignmentRequest(@FutureOrPresent LocalDate dueDate, AssignmentPriority priority) {
	public boolean hasChanges() { return dueDate != null || priority != null; }
}
