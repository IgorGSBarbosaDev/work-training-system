package dev.igorbarbosa.worktrainingsystem.assignments.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainingAssignmentTest {
	@Test
	void cancelAndWaiveCloseOnlyExecutableAssignmentsAndPreserveMetadata() {
		UUID actor = UUID.randomUUID(); Instant now = Instant.parse("2026-07-24T12:00:00Z");
		TrainingAssignment cancelled = assignment(actor);
		cancelled.cancel(actor, "Não aplicável", now);
		assertThat(cancelled.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
		assertThat(cancelled.getCancellationReason()).isEqualTo("Não aplicável");
		assertThatThrownBy(() -> cancelled.waive(actor, "Dispensa", now)).isInstanceOf(IllegalStateException.class);

		TrainingAssignment waived = assignment(actor);
		waived.waive(actor, "Dispensa administrativa", now);
		assertThat(waived.getStatus()).isEqualTo(AssignmentStatus.WAIVED);
		assertThat(waived.getWaiverReason()).isEqualTo("Dispensa administrativa");
	}

	@Test
	void followsLearnerExecutionTransitionsOnly() {
		TrainingAssignment assignment = assignment(UUID.randomUUID());
		assignment.start();
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
		assignment.awaitAssessment();
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AWAITING_ASSESSMENT);
		assertThatThrownBy(assignment::start).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void retainsFailureForRetryAndCompletesOnlyAfterApprovedAssessment() {
		TrainingAssignment assignment = assignment(UUID.randomUUID());
		assignment.start(); assignment.awaitAssessment(); assignment.assessmentResult(false, false);
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.FAILED);
		assignment.assessmentResult(true, true);
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.APPROVED);
		assignment.complete();
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
		assertThatThrownBy(() -> assignment.assessmentResult(false, false)).isInstanceOf(IllegalStateException.class);

		TrainingAssignment partial = assignment(UUID.randomUUID());
		partial.start(); partial.awaitAssessment(); partial.assessmentResult(true, false);
		assertThat(partial.getStatus()).isEqualTo(AssignmentStatus.AWAITING_ASSESSMENT);
	}

	private TrainingAssignment assignment(UUID actor) {
		return new TrainingAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				AssignmentOrigin.EMPLOYEE, Instant.now(), null, AssignmentPriority.NORMAL, actor, null, null, null, null);
	}
}
