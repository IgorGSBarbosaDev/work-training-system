package dev.igorbarbosa.worktrainingsystem.assessments.domain;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
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

@Entity
@Table(name = "completion_expiration_history")
public class CompletionExpirationHistory {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "completion_id", nullable = false, updatable = false) private UUID completionId;
	@Column(name = "previous_expiration_date", updatable = false) private LocalDate previousExpirationDate;
	@Column(name = "recalculated_expiration_date", updatable = false) private LocalDate recalculatedExpirationDate;
	@Enumerated(EnumType.STRING) @Column(name = "validity_type", nullable = false, updatable = false) private ValidityType validityType;
	@Column(name = "validity_value", updatable = false) private Integer validityValue;
	@Column(name = "responsible_user_id", nullable = false, updatable = false) private UUID responsibleUserId;
	@Column(updatable = false, length = 1000) private String reason;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected CompletionExpirationHistory() {}
	public CompletionExpirationHistory(UUID organizationId, UUID completionId, LocalDate previousExpirationDate,
			LocalDate recalculatedExpirationDate, ValidityType validityType, Integer validityValue,
			UUID responsibleUserId, String reason, Instant createdAt) {
		this.organizationId = organizationId; this.completionId = completionId;
		this.previousExpirationDate = previousExpirationDate; this.recalculatedExpirationDate = recalculatedExpirationDate;
		this.validityType = validityType; this.validityValue = validityValue;
		this.responsibleUserId = responsibleUserId; this.reason = reason; this.createdAt = createdAt;
	}
	public LocalDate getRecalculatedExpirationDate() { return recalculatedExpirationDate; }
	public ValidityType getValidityType() { return validityType; }
	public Integer getValidityValue() { return validityValue; }
}
