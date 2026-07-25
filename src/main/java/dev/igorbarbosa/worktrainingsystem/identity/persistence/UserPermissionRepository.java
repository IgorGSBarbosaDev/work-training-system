package dev.igorbarbosa.worktrainingsystem.identity.persistence;

import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserPermission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, UUID> {
	List<UserPermission> findAllByUserId(UUID userId);
	boolean existsByUserIdAndPermission(UUID userId, Permission permission);
	void deleteByUserId(UUID userId);
}
