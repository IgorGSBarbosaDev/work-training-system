package dev.igorbarbosa.worktrainingsystem.certificates.api;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateGenerationStatus;
import java.time.Instant;
import java.util.UUID;

public record CertificateJobResponse(UUID id, UUID completionId, CertificateGenerationStatus status,
		int attemptCount, String lastError, UUID certificateId, Instant updatedAt) {}
