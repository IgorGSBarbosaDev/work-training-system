package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.RefreshTokenFamily;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, UUID> {
	List<RefreshTokenFamily> findAllByUserIdAndRevokedAtIsNull(UUID userId);
	void deleteByExpiresAtBefore(Instant cutoff);
}
