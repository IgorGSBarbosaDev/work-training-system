package dev.igorbarbosa.worktrainingsystem.identity.application;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EmployeeIdentityPort {
	Optional<EmployeeScope> findScope(UUID organizationId, UUID employeeId);
	List<EmployeeScope> findActiveScopes(UUID organizationId, Set<UUID> unitIds, Set<UUID> sectorIds,
			Set<UUID> employeeIds);

	record EmployeeScope(UUID employeeId, UUID organizationId, UUID unitId, UUID sectorId, UUID jobId,
			boolean active) {}
}
