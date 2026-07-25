package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID organizationId, UserRole role, UUID employeeId,
		Set<Permission> permissions) {
	public boolean hasPermission(Permission permission) { return permissions.contains(permission); }
}
