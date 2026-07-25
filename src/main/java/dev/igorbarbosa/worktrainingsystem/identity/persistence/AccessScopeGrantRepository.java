package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.AccessScopeGrant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessScopeGrantRepository extends JpaRepository<AccessScopeGrant, UUID> {
	List<AccessScopeGrant> findAllByUserIdAndActiveTrue(UUID userId);
	void deleteByUserId(UUID userId);
}
