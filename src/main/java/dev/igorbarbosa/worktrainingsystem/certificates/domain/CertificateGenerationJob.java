package dev.igorbarbosa.worktrainingsystem.certificates.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "certificate_generation_jobs")
public class CertificateGenerationJob {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "completion_id", nullable = false, updatable = false) private UUID completionId;
	@Enumerated(EnumType.STRING) @Column(name = "certificate_type", nullable = false, updatable = false) private CertificateType certificateType;
	@Column(name = "requested_by_user_id", nullable = false, updatable = false) private UUID requestedByUserId;
	@Column(name = "replaces_certificate_id", updatable = false) private UUID replacesCertificateId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private CertificateGenerationStatus status;
	@Column(name = "attempt_count", nullable = false) private int attemptCount;
	@Column(name = "last_error", length = 1000) private String lastError;
	@Column(name = "certificate_id") private UUID certificateId;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected CertificateGenerationJob() {}
	public CertificateGenerationJob(UUID organizationId, UUID completionId, CertificateType type,
			UUID requestedByUserId, UUID replacesCertificateId, Instant now) {
		this.organizationId = organizationId; this.completionId = completionId; this.certificateType = type;
		this.requestedByUserId = requestedByUserId; this.replacesCertificateId = replacesCertificateId;
		this.status = CertificateGenerationStatus.PENDING; this.createdAt = now; this.updatedAt = now;
	}
	public void processing(Instant now) { status = CertificateGenerationStatus.PROCESSING; attemptCount++; lastError = null; updatedAt = now; }
	public void completed(UUID value, Instant now) { status = CertificateGenerationStatus.COMPLETED; certificateId = value; updatedAt = now; }
	public void failed(String error, Instant now) { status = CertificateGenerationStatus.FAILED; lastError = error; updatedAt = now; }
	public void retry(Instant now) { status = CertificateGenerationStatus.PENDING; lastError = null; updatedAt = now; }
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getCompletionId() { return completionId; }
	public CertificateType getCertificateType() { return certificateType; }
	public UUID getRequestedByUserId() { return requestedByUserId; }
	public UUID getReplacesCertificateId() { return replacesCertificateId; }
	public CertificateGenerationStatus getStatus() { return status; }
	public int getAttemptCount() { return attemptCount; }
	public String getLastError() { return lastError; }
	public UUID getCertificateId() { return certificateId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
