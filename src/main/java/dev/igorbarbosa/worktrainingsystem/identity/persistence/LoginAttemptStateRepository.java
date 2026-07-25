package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.LoginAttemptState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LoginAttemptStateRepository extends JpaRepository<LoginAttemptState, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<LoginAttemptState> findByEmailHash(String emailHash);
	void deleteByEmailHash(String emailHash);
}
