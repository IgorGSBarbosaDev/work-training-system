package dev.igorbarbosa.worktrainingsystem.employees.application;

import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.EmployeeIdentityPort;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class EmployeeIdentityAdapter implements EmployeeIdentityPort {
	private final EmployeeRepository employeeRepository;
	EmployeeIdentityAdapter(EmployeeRepository employeeRepository) { this.employeeRepository = employeeRepository; }

	@Override
	public Optional<EmployeeScope> findScope(UUID organizationId, UUID employeeId) {
		return employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
				.map(employee -> new EmployeeScope(employee.getId(), employee.getOrganizationId(),
						employee.getUnitId(), employee.getSectorId(), employee.getJobId(),
						employee.getStatus() == RegistrationStatus.ACTIVE));
	}

	@Override
	public List<EmployeeScope> findActiveScopes(UUID organizationId, Set<UUID> unitIds, Set<UUID> sectorIds,
			Set<UUID> employeeIds) {
		if (unitIds.isEmpty() && sectorIds.isEmpty() && employeeIds.isEmpty()) return List.of();
		return employeeRepository.findActiveInScope(organizationId, unitIds, sectorIds, employeeIds).stream()
				.map(employee -> new EmployeeScope(employee.getId(), employee.getOrganizationId(), employee.getUnitId(),
						employee.getSectorId(), employee.getJobId(), true))
				.toList();
	}
}
