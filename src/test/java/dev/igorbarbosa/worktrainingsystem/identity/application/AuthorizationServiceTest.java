package dev.igorbarbosa.worktrainingsystem.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.identity.domain.AccessScopeGrant;
import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AccessScopeGrantRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {
	private final CurrentUserProvider currentUsers = mock(CurrentUserProvider.class);
	private final AccessScopeGrantRepository grants = mock(AccessScopeGrantRepository.class);
	private final EmployeeIdentityPort employees = mock(EmployeeIdentityPort.class);
	private final AuthorizationService authorization = new AuthorizationService(currentUsers, grants, employees);

	@Test
	void adminCanAccessAllAndEmployeeOnlySelf() {
		UUID target = UUID.randomUUID();
		CurrentUser admin = current(UserRole.ADMIN, null);
		CurrentUser employee = current(UserRole.EMPLOYEE, target);
		assertThat(authorization.canAccessEmployee(admin, UUID.randomUUID())).isTrue();
		assertThat(authorization.canAccessEmployee(employee, target)).isTrue();
		assertThat(authorization.canAccessEmployee(employee, UUID.randomUUID())).isFalse();
	}

	@Test
	void managerRequiresMatchingActiveDirectGrant() {
		UUID employeeId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		UUID sectorId = UUID.randomUUID();
		CurrentUser manager = current(UserRole.MANAGER, null);
		when(employees.findScope(manager.organizationId(), employeeId)).thenReturn(Optional.of(
				new EmployeeIdentityPort.EmployeeScope(employeeId, manager.organizationId(), unitId, sectorId,
						UUID.randomUUID(), true)));
		when(grants.findAllByUserIdAndActiveTrue(manager.userId())).thenReturn(List.of(
				new AccessScopeGrant(manager.userId(), manager.organizationId(), ScopeType.UNIT, unitId)));
		assertThat(authorization.canAccessEmployee(manager, employeeId)).isTrue();

		when(grants.findAllByUserIdAndActiveTrue(manager.userId())).thenReturn(List.of());
		assertThat(authorization.canAccessEmployee(manager, employeeId)).isFalse();
	}

	@Test
	void permissionIsExplicitForManagerButImplicitForAdmin() {
		when(currentUsers.requireCurrentUser()).thenReturn(current(UserRole.MANAGER, null));
		assertThat(authorization.hasPermission("ASSIGN_TRAINING")).isFalse();
		when(currentUsers.requireCurrentUser()).thenReturn(new CurrentUser(UUID.randomUUID(), UUID.randomUUID(),
				UserRole.MANAGER, null, Set.of(Permission.ASSIGN_TRAINING)));
		assertThat(authorization.hasPermission("ASSIGN_TRAINING")).isTrue();
		when(currentUsers.requireCurrentUser()).thenReturn(current(UserRole.ADMIN, null));
		assertThat(authorization.hasPermission("ASSIGN_TRAINING")).isTrue();
	}

	private CurrentUser current(UserRole role, UUID employeeId) {
		return new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), role, employeeId, Set.of());
	}
}
