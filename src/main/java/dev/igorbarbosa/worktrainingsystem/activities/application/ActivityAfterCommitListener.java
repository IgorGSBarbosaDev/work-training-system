package dev.igorbarbosa.worktrainingsystem.activities.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class ActivityAfterCommitListener {
	private final AssignmentGenerationPort assignments;
	private final QualificationRecalculationPort qualifications;
	ActivityAfterCommitListener(AssignmentGenerationPort assignments, QualificationRecalculationPort qualifications) {
		this.assignments = assignments; this.qualifications = qualifications;
	}
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void assignments(ActivityAssignmentRequested event) { assignments.generate(event); }
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void qualifications(QualificationRecalculationRequested event) { qualifications.recalculate(event); }
}
