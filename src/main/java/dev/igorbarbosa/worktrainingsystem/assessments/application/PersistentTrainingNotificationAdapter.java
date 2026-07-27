package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AssessmentAttemptRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.notifications.application.SliceBNotificationPort;
import org.springframework.stereotype.Component;

@Component
class PersistentTrainingNotificationAdapter implements TrainingNotificationPort {
	private final TrainingCompletionRepository completions;
	private final AssessmentAttemptRepository attempts;
	private final SliceBNotificationPort notifications;

	PersistentTrainingNotificationAdapter(TrainingCompletionRepository completions,
			AssessmentAttemptRepository attempts, SliceBNotificationPort notifications) {
		this.completions = completions;
		this.attempts = attempts;
		this.notifications = notifications;
	}

	@Override
	public void outcomeRecorded(TrainingOutcomeEvent event) {
		if (event.completionId() != null) {
			completions.findByIdAndOrganizationId(event.completionId(), DEFAULT_ORGANIZATION_ID)
					.map(completion -> completion.getSourceAssignmentId())
					.ifPresent(assignmentId -> notifications.trainingCompleted(
							new SliceBNotificationPort.AssignmentNotification(DEFAULT_ORGANIZATION_ID,
									event.employeeId(), assignmentId, event.trainingId())));
		}
		if (event.failedAttemptId() != null) {
			attempts.findByIdAndOrganizationId(event.failedAttemptId(), DEFAULT_ORGANIZATION_ID)
					.ifPresent(attempt -> notifications.assessmentFailed(
							new SliceBNotificationPort.AssignmentNotification(DEFAULT_ORGANIZATION_ID,
									event.employeeId(), attempt.getAssignmentId(), event.trainingId())));
		}
	}
}
