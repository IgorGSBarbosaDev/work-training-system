package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "login_attempt_states")
public class LoginAttemptState extends BaseEntity {
	@Column(name = "email_hash", nullable = false, updatable = false, length = 64)
	private String emailHash;
	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;
	@Column(name = "window_started_at", nullable = false)
	private Instant windowStartedAt;
	@Column(name = "locked_until")
	private Instant lockedUntil;
	@Column(name = "last_attempt_at", nullable = false)
	private Instant lastAttemptAt;

	protected LoginAttemptState() {}

	public LoginAttemptState(String emailHash, Instant now) {
		this.emailHash = emailHash;
		this.windowStartedAt = now;
		this.lastAttemptAt = now;
	}

	public boolean isLocked(Instant now) { return lockedUntil != null && lockedUntil.isAfter(now); }

	public void recordFailure(Instant now, Duration window, int threshold, Duration lockDuration) {
		if (!windowStartedAt.plus(window).isAfter(now)) {
			failedAttempts = 0;
			windowStartedAt = now;
			lockedUntil = null;
		}
		failedAttempts++;
		lastAttemptAt = now;
		if (failedAttempts >= threshold) lockedUntil = now.plus(lockDuration);
	}

	public String getEmailHash() { return emailHash; }
	public int getFailedAttempts() { return failedAttempts; }
	public Instant getWindowStartedAt() { return windowStartedAt; }
	public Instant getLockedUntil() { return lockedUntil; }
	public Instant getLastAttemptAt() { return lastAttemptAt; }
}
