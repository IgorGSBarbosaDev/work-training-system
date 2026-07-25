package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_families")
public class RefreshTokenFamily extends BaseEntity {
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;
	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;
	@Column(name = "revoked_at")
	private Instant revokedAt;
	@Column(name = "revocation_reason", length = 64)
	private String revocationReason;

	protected RefreshTokenFamily() {}
	public RefreshTokenFamily(UUID userId, Instant expiresAt) { this.userId = userId; this.expiresAt = expiresAt; }
	public void revoke(Instant now, String reason) { if (revokedAt == null) { revokedAt = now; revocationReason = reason; } }
	public boolean isUsable(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
	public UUID getUserId() { return userId; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getRevokedAt() { return revokedAt; }
}
