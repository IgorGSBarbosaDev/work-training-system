package dev.igorbarbosa.worktrainingsystem.expirations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "completion_expiration_states") @IdClass(CompletionExpirationState.Key.class)
public class CompletionExpirationState {
	@Id @Column(name = "completion_id") private UUID completionId;
	@Id @Column(name = "organization_id") private UUID organizationId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ExpirationStatus status;
	@Column(name = "evaluated_at", nullable = false) private Instant evaluatedAt;
	protected CompletionExpirationState() {}
	public CompletionExpirationState(UUID completionId, UUID organizationId, ExpirationStatus status, Instant evaluatedAt) {
		this.completionId = completionId; this.organizationId = organizationId; this.status = status; this.evaluatedAt = evaluatedAt;
	}
	public void evaluate(ExpirationStatus value, Instant at) { status = value; evaluatedAt = at; }
	public ExpirationStatus getStatus() { return status; }
	public record Key(UUID completionId, UUID organizationId) implements Serializable {}
}
