package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.domain.AccessScopeGrant;
import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AccessScopeGrantRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("authorization")
public class AuthorizationService {
	private final CurrentUserProvider currentUserProvider;
	private final AccessScopeGrantRepository scopeRepository;
	private final EmployeeIdentityPort employeePort;

	public AuthorizationService(CurrentUserProvider currentUserProvider,
			AccessScopeGrantRepository scopeRepository, EmployeeIdentityPort employeePort) {
		this.currentUserProvider = currentUserProvider; this.scopeRepository = scopeRepository; this.employeePort = employeePort;
	}

	public boolean hasPermission(String permission) {
		CurrentUser current = currentUserProvider.requireCurrentUser();
		return current.role() == UserRole.ADMIN || current.hasPermission(Permission.valueOf(permission));
	}

	@Transactional(readOnly = true)
	public boolean canAccessEmployee(UUID employeeId) {
		CurrentUser current = currentUserProvider.requireCurrentUser();
		return canAccessEmployee(current, employeeId);
	}

	@Transactional(readOnly = true)
	public boolean canAccessEmployee(CurrentUser current, UUID employeeId) {
		if (current.role() == UserRole.ADMIN) return true;
		if (current.role() == UserRole.EMPLOYEE) return employeeId.equals(current.employeeId());
		if (current.role() != UserRole.MANAGER && current.role() != UserRole.SUPERVISOR) return false;
		EmployeeIdentityPort.EmployeeScope employee = employeePort.findScope(current.organizationId(), employeeId).orElse(null);
		if (employee == null || !employee.active()) return false;
		List<AccessScopeGrant> grants = scopeRepository.findAllByUserIdAndActiveTrue(current.userId());
		return grants.stream().anyMatch(grant -> switch (grant.getScopeType()) {
			case EMPLOYEE -> grant.targetId().equals(employee.employeeId());
			case UNIT -> grant.targetId().equals(employee.unitId());
			case SECTOR -> grant.targetId().equals(employee.sectorId());
		});
	}

	@Transactional(readOnly = true)
	public AccessScope currentScope() {
		CurrentUser current = currentUserProvider.requireCurrentUser();
		if (current.role() == UserRole.ADMIN) {
			return new AccessScope(current.userId(), current.organizationId(), current.role(), current.employeeId(), true,
					Set.of(), Set.of(), Set.of());
		}
		List<AccessScopeGrant> grants = current.role() == UserRole.MANAGER || current.role() == UserRole.SUPERVISOR
				? scopeRepository.findAllByUserIdAndActiveTrue(current.userId()) : List.of();
		Set<UUID> unitIds = targets(grants, dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType.UNIT);
		Set<UUID> sectorIds = targets(grants, dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType.SECTOR);
		Set<UUID> employeeIds = targets(grants, dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType.EMPLOYEE);
		return new AccessScope(current.userId(), current.organizationId(), current.role(), current.employeeId(), false,
				unitIds, sectorIds, employeeIds);
	}

	@Transactional(readOnly = true)
	public ScopeReferences scopeReferences(AccessScope scope) {
		if (scope.admin() || !scope.manager() || !scope.hasGrants()) {
			return new ScopeReferences(Set.of(), Set.of(), Set.of(), Set.of());
		}
		List<EmployeeIdentityPort.EmployeeScope> employees = employeePort.findActiveScopes(scope.organizationId(),
				scope.unitIds(), scope.sectorIds(), scope.employeeIds());
		return new ScopeReferences(
				employees.stream().map(EmployeeIdentityPort.EmployeeScope::employeeId).collect(Collectors.toUnmodifiableSet()),
				employees.stream().map(EmployeeIdentityPort.EmployeeScope::unitId).collect(Collectors.toUnmodifiableSet()),
				employees.stream().map(EmployeeIdentityPort.EmployeeScope::sectorId).collect(Collectors.toUnmodifiableSet()),
				employees.stream().map(EmployeeIdentityPort.EmployeeScope::jobId).collect(Collectors.toUnmodifiableSet()));
	}

	private Set<UUID> targets(List<AccessScopeGrant> grants,
			dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType type) {
		return grants.stream().filter(grant -> grant.getScopeType() == type).map(AccessScopeGrant::targetId)
				.collect(Collectors.toUnmodifiableSet());
	}

	public record AccessScope(UUID userId, UUID organizationId, UserRole role, UUID ownEmployeeId, boolean admin,
			Set<UUID> unitIds, Set<UUID> sectorIds, Set<UUID> employeeIds) {
		public boolean manager() { return role == UserRole.MANAGER || role == UserRole.SUPERVISOR; }
		public boolean employee() { return role == UserRole.EMPLOYEE; }
		public boolean hasGrants() { return !unitIds.isEmpty() || !sectorIds.isEmpty() || !employeeIds.isEmpty(); }
	}

	public record ScopeReferences(Set<UUID> employeeIds, Set<UUID> unitIds, Set<UUID> sectorIds, Set<UUID> jobIds) {}
}
