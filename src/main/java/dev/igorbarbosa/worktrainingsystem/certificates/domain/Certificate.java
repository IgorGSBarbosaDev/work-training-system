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
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "certificates")
public class Certificate {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "completion_id", nullable = false, updatable = false) private UUID completionId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 16) private CertificateType type;
	@Column(name = "validation_code", nullable = false, updatable = false, length = 64) private String validationCode;
	@Column(name = "object_key", nullable = false, updatable = false, length = 1024) private String objectKey;
	@Column(name = "issued_date", nullable = false, updatable = false) private LocalDate issuedDate;
	@Column(name = "issued_at", nullable = false, updatable = false) private Instant issuedAt;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private CertificateStatus status;
	@Column(name = "responsible_user_id", nullable = false, updatable = false) private UUID responsibleUserId;
	@Column(name = "revoked_at") private Instant revokedAt;
	@Column(name = "revoked_by_user_id") private UUID revokedByUserId;
	@Column(name = "revocation_reason", length = 1000) private String revocationReason;
	@Column(name = "previous_certificate_id", updatable = false) private UUID previousCertificateId;
	@Column(name = "generation_number", nullable = false, updatable = false) private int generationNumber;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected Certificate() {}
	public Certificate(UUID organizationId, UUID completionId, CertificateType type, String validationCode,
			String objectKey, Instant issuedAt, UUID responsibleUserId, UUID previousCertificateId, int generationNumber) {
		this.organizationId = organizationId; this.completionId = completionId; this.type = type;
		this.validationCode = validationCode; this.objectKey = objectKey; this.issuedAt = issuedAt;
		this.issuedDate = LocalDate.ofInstant(issuedAt, java.time.ZoneOffset.UTC); this.status = CertificateStatus.ACTIVE;
		this.responsibleUserId = responsibleUserId; this.previousCertificateId = previousCertificateId;
		this.generationNumber = generationNumber; this.createdAt = issuedAt;
	}
	public void revoke(UUID actor, String reason, Instant now) {
		if (status == CertificateStatus.REVOKED) return;
		status = CertificateStatus.REVOKED; revokedAt = now; revokedByUserId = actor; revocationReason = reason;
	}
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getCompletionId() { return completionId; }
	public CertificateType getType() { return type; }
	public String getValidationCode() { return validationCode; }
	public String getObjectKey() { return objectKey; }
	public LocalDate getIssuedDate() { return issuedDate; }
	public Instant getIssuedAt() { return issuedAt; }
	public CertificateStatus getStatus() { return status; }
	public UUID getResponsibleUserId() { return responsibleUserId; }
	public Instant getRevokedAt() { return revokedAt; }
	public UUID getRevokedByUserId() { return revokedByUserId; }
	public String getRevocationReason() { return revocationReason; }
	public UUID getPreviousCertificateId() { return previousCertificateId; }
	public int getGenerationNumber() { return generationNumber; }
}
