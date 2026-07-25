package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {
	@Column(name = "family_id", nullable = false, updatable = false)
	private UUID familyId;
	@Column(name = "token_hash", nullable = false, updatable = false, length = 64)
	private String tokenHash;
	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;
	@Column(name = "used_at")
	private Instant usedAt;
	@Column(name = "revoked_at")
	private Instant revokedAt;
	@Column(name = "replaced_by_id")
	private UUID replacedById;

	protected RefreshToken() {}
	public RefreshToken(UUID familyId, String tokenHash, Instant expiresAt) {
		this.familyId = familyId; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
	}
	public boolean isFresh(Instant now) { return usedAt == null && revokedAt == null && expiresAt.isAfter(now); }
	public void rotate(Instant now, UUID replacementId) { usedAt = now; replacedById = replacementId; }
	public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
	public UUID getFamilyId() { return familyId; }
	public String getTokenHash() { return tokenHash; }
	public Instant getUsedAt() { return usedAt; }
	public Instant getRevokedAt() { return revokedAt; }
	public Instant getExpiresAt() { return expiresAt; }
}
