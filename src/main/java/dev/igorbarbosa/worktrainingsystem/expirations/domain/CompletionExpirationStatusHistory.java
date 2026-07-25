package dev.igorbarbosa.worktrainingsystem.expirations.domain;

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

@Entity @Table(name = "completion_expiration_status_history")
public class CompletionExpirationStatusHistory {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "completion_id", nullable = false, updatable = false) private UUID completionId;
	@Enumerated(EnumType.STRING) @Column(name = "previous_status", updatable = false) private ExpirationStatus previousStatus;
	@Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, updatable = false) private ExpirationStatus newStatus;
	@Column(name = "effective_expiration_date", nullable = false, updatable = false) private LocalDate effectiveExpirationDate;
	@Column(name = "recorded_at", nullable = false, updatable = false) private Instant recordedAt;
	protected CompletionExpirationStatusHistory() {}
	public CompletionExpirationStatusHistory(UUID organizationId, UUID completionId, ExpirationStatus previousStatus,
			ExpirationStatus newStatus, LocalDate effectiveExpirationDate, Instant recordedAt) {
		this.organizationId = organizationId; this.completionId = completionId; this.previousStatus = previousStatus;
		this.newStatus = newStatus; this.effectiveExpirationDate = effectiveExpirationDate; this.recordedAt = recordedAt;
	}
}
