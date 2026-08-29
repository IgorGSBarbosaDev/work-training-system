package dev.igorbarbosa.worktrainingsystem.assessments.application;

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
			completions.findById(event.completionId()).ifPresent(completion ->
				notifications.trainingCompleted(new SliceBNotificationPort.AssignmentNotification(
						completion.getOrganizationId(), event.employeeId(), completion.getSourceAssignmentId(),
						event.trainingId())));
		}
		if (event.failedAttemptId() != null) {
			attempts.findById(event.failedAttemptId())
					.ifPresent(attempt -> notifications.assessmentFailed(
							new SliceBNotificationPort.AssignmentNotification(attempt.getOrganizationId(),
									event.employeeId(), attempt.getAssignmentId(), event.trainingId())));
		}
	}
}
