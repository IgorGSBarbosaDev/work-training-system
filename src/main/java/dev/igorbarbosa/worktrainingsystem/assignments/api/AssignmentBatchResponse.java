package dev.igorbarbosa.worktrainingsystem.assignments.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchResultType;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchStatus;
import java.util.List;
import java.util.UUID;

public record AssignmentBatchResponse(UUID batchId, AssignmentBatchStatus status, int requested,
		int created, int skipped, int failed, List<Result> results) {
	public record Result(UUID employeeId, AssignmentBatchResultType result, UUID assignmentId,
			String code, String message) {}
}
