package dev.igorbarbosa.worktrainingsystem.certificates.api;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateStatus;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertificateResponse(UUID id, UUID completionId, CertificateType type, String validationCode,
		LocalDate issuedDate, Instant issuedAt, CertificateStatus status, UUID responsibleUserId,
		Instant revokedAt, UUID revokedByUserId, String revocationReason, UUID previousCertificateId,
		int generationNumber) {}
