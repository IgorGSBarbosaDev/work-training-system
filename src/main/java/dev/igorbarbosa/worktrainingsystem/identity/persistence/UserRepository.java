package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<User> findByEmailIgnoreCase(String email);
	Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Page<User> findAllByOrganizationId(UUID organizationId, Pageable pageable);
	boolean existsByEmailIgnoreCase(String email);
}
