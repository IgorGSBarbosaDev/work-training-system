package dev.igorbarbosa.worktrainingsystem.assessments.application;

import java.util.UUID;

public record TrainingOutcomeEvent(UUID employeeId, UUID trainingId, UUID completionId, UUID failedAttemptId) {
	public static TrainingOutcomeEvent completed(UUID employeeId, UUID trainingId, UUID completionId) {
		return new TrainingOutcomeEvent(employeeId, trainingId, completionId, null);
	}
	public static TrainingOutcomeEvent failed(UUID employeeId, UUID trainingId, UUID attemptId) {
		return new TrainingOutcomeEvent(employeeId, trainingId, null, attemptId);
	}
}
