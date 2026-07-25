package dev.igorbarbosa.worktrainingsystem.assessments.application;

import java.util.UUID;

/** Phase 5 certificate boundary. */
public interface CertificateIssuancePort {
	void completionRecorded(UUID completionId);
}
