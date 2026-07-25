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

@Entity @Table(name = "certificate_history")
public class CertificateHistory {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "certificate_id", nullable = false, updatable = false) private UUID certificateId;
	@Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, updatable = false) private CertificateHistoryType eventType;
	@Column(name = "responsible_user_id", nullable = false, updatable = false) private UUID responsibleUserId;
	@Column(name = "related_certificate_id", updatable = false) private UUID relatedCertificateId;
	@Column(updatable = false, length = 1000) private String reason;
	@Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
	protected CertificateHistory() {}
	public CertificateHistory(UUID organizationId, UUID certificateId, CertificateHistoryType eventType,
			UUID responsibleUserId, UUID relatedCertificateId, String reason, Instant occurredAt) {
		this.organizationId = organizationId; this.certificateId = certificateId; this.eventType = eventType;
		this.responsibleUserId = responsibleUserId; this.relatedCertificateId = relatedCertificateId;
		this.reason = reason; this.occurredAt = occurredAt;
	}
}
