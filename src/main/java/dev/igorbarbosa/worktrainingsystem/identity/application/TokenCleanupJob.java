package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.persistence.PasswordResetTokenRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenFamilyRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class TokenCleanupJob {
	private final RefreshTokenRepository refreshTokens;
	private final RefreshTokenFamilyRepository families;
	private final PasswordResetTokenRepository resetTokens;
	private final Clock clock;
	TokenCleanupJob(RefreshTokenRepository refreshTokens, RefreshTokenFamilyRepository families,
			PasswordResetTokenRepository resetTokens, Clock clock) {
		this.refreshTokens = refreshTokens; this.families = families; this.resetTokens = resetTokens; this.clock = clock;
	}

	@Scheduled(cron = "${app.identity.token-cleanup-cron:0 0 3 * * *}")
	@Transactional
	void removeExpiredTokens() {
		refreshTokens.deleteByExpiresAtBefore(clock.instant());
		families.deleteByExpiresAtBefore(clock.instant());
		resetTokens.deleteByExpiresAtBefore(clock.instant());
	}
}
