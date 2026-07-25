package dev.igorbarbosa.worktrainingsystem.assessments.application;

/** Phase 5 notification boundary. */
public interface TrainingNotificationPort {
	void outcomeRecorded(TrainingOutcomeEvent event);
}
