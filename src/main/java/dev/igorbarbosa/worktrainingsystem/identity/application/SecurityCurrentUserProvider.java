package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
class SecurityCurrentUserProvider implements CurrentUserProvider {
	@Override
	public CurrentUser requireCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw IdentityAuthenticationException.invalidToken();
		}
		if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
			UserRole role = authentication.getAuthorities().stream()
					.map(authority -> authority.getAuthority())
					.filter(value -> value.startsWith("ROLE_"))
					.map(value -> UserRole.valueOf(value.substring(5)))
					.findFirst().orElseThrow(IdentityAuthenticationException::invalidToken);
			UUID userId;
			try { userId = UUID.fromString(authentication.getName()); }
			catch (IllegalArgumentException exception) {
				userId = UUID.nameUUIDFromBytes(authentication.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			return new CurrentUser(userId,
					dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID,
					role, null, Set.of());
		}
		Set<Permission> permissions = new HashSet<>();
		java.util.List<String> values = jwt.getClaimAsStringList("permissions");
		if (values != null) for (String value : values) permissions.add(Permission.valueOf(value));
		String employeeId = jwt.getClaimAsString("employee_id");
		return new CurrentUser(UUID.fromString(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("org")),
				UserRole.valueOf(jwt.getClaimAsString("role")), employeeId == null ? null : UUID.fromString(employeeId),
				Set.copyOf(permissions));
	}
}
