package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;
	@Column(name = "token_hash", nullable = false, updatable = false, length = 64)
	private String tokenHash;
	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;
	@Column(name = "used_at")
	private Instant usedAt;
	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected PasswordResetToken() {}
	public PasswordResetToken(UUID userId, String tokenHash, Instant expiresAt) {
		this.userId = userId; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
	}
	public boolean isUsable(Instant now) { return usedAt == null && revokedAt == null && expiresAt.isAfter(now); }
	public void use(Instant now) { usedAt = now; }
	public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
	public UUID getUserId() { return userId; }
}
