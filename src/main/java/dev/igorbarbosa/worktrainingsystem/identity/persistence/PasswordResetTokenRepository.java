package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
	List<PasswordResetToken> findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(UUID userId);
	void deleteByExpiresAtBefore(Instant cutoff);
}
