package dev.igorbarbosa.worktrainingsystem.qualifications.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Phase 4 replaces the conservative adapter with completion and assessment persistence. */
public interface TrainingCompliancePort {
	ComplianceStatus status(UUID employeeId, UUID trainingId);

	record ComplianceStatus(Optional<CompletionEvidence> completion, boolean unresolvedFailedAssessment) {
		public ComplianceStatus {
			completion = completion == null ? Optional.empty() : completion;
		}
		public static ComplianceStatus missing() { return new ComplianceStatus(Optional.empty(), false); }
	}
	record CompletionEvidence(UUID completionId, UUID trainingVersionId, Instant completedAt,
			LocalDate expirationDate) {}
}
