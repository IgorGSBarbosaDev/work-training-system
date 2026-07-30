package dev.igorbarbosa.worktrainingsystem.assessments.application;

import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class TrainingOutcomeAfterCommitListener {
	private final QualificationCommandPort qualifications;
	private final CertificateIssuancePort certificates;
	private final TrainingNotificationPort notifications;
	TrainingOutcomeAfterCommitListener(QualificationCommandPort qualifications, CertificateIssuancePort certificates,
			TrainingNotificationPort notifications) {
		this.qualifications = qualifications; this.certificates = certificates; this.notifications = notifications;
	}
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void onOutcome(TrainingOutcomeEvent event) {
		qualifications.recalculateEmployee(event.employeeId());
		if (event.completionId() != null) certificates.completionRecorded(event.completionId());
		notifications.outcomeRecorded(event);
	}
}
