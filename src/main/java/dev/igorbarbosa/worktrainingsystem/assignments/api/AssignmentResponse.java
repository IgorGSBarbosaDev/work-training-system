package dev.igorbarbosa.worktrainingsystem.assignments.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
		UUID id,
		Reference employee,
		Reference training,
		UUID trainingVersionId,
		int trainingVersion,
		AssignmentOrigin origin,
		List<Source> sources,
		Instant assignedAt,
		LocalDate assignedDate,
		LocalDate dueDate,
		AssignmentStatus status,
		AssignmentPriority priority,
		UUID responsibleUserId,
		boolean recertification,
		UUID recertificationOfAssignmentId,
		Instant cancelledAt,
		String cancellationReason,
		Instant waivedAt,
		String waiverReason,
		UUID batchId,
		Instant createdAt,
		Instant updatedAt) {
	public record Reference(UUID id, String name) {}
	public record Source(AssignmentOrigin origin, UUID referenceId) {}
}
