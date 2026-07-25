package dev.igorbarbosa.worktrainingsystem.assignments.application;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.util.Optional;
import java.util.UUID;

/** Public boundary for assignment execution; progress never accesses assignment persistence directly. */
public interface AssignmentExecutionPort {
	ExecutionAssignment view(UUID assignmentId);
	ExecutionAssignment requireOwner(UUID assignmentId, boolean mustBeInProgress);
	ExecutionAssignment start(UUID assignmentId);
	ExecutionAssignment contentReady(UUID assignmentId, boolean hasActiveQuestionnaires);
	ExecutionAssignment lockForAssessment(UUID assignmentId);
	ExecutionAssignment assessmentResult(UUID assignmentId, boolean approved, boolean allQuestionnairesPassed);
	ExecutionAssignment complete(UUID assignmentId, String reason);
	Optional<ExecutionAssignment> findPlaybackAssignment(UUID employeeId, UUID trainingVersionId);

	record ExecutionAssignment(UUID id, UUID organizationId, UUID employeeId, UUID trainingId,
			UUID trainingVersionId, AssignmentStatus status) {
		public boolean executable() {
			return status == AssignmentStatus.NOT_STARTED || status == AssignmentStatus.IN_PROGRESS
					|| status == AssignmentStatus.AWAITING_ASSESSMENT || status == AssignmentStatus.FAILED;
		}
	}
}
