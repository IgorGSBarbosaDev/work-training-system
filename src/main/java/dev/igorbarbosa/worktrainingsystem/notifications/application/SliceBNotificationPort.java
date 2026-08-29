package dev.igorbarbosa.worktrainingsystem.notifications.application;

import java.time.LocalDate;
import java.util.UUID;

/** Events consumed by the durable notification implementation in Phase 5 slice B. */
public interface SliceBNotificationPort {
	void expirationChanged(ExpirationNotification event);
	void qualificationBlocked(QualificationBlockedNotification event);
	default void assignmentCreated(AssignmentNotification event) {}
	default void assignmentDue(AssignmentNotification event) {}
	default void assessmentFailed(AssignmentNotification event) {}
	default void trainingCompleted(AssignmentNotification event) {}

	record ExpirationNotification(UUID organizationId, UUID employeeId, UUID trainingId,
			UUID completionId, LocalDate expirationDate, String status) {}
	record QualificationBlockedNotification(UUID organizationId, UUID employeeId, UUID activityId) {}
	record AssignmentNotification(UUID organizationId, UUID employeeId, UUID assignmentId, UUID trainingId,
			LocalDate effectiveDate) {
		public AssignmentNotification(UUID organizationId, UUID employeeId, UUID assignmentId, UUID trainingId) {
			this(organizationId, employeeId, assignmentId, trainingId, null);
		}
	}
}
