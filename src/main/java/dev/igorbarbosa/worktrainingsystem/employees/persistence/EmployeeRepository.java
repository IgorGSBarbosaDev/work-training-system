package dev.igorbarbosa.worktrainingsystem.employees.persistence;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

	boolean existsByOrganizationIdAndRegistrationIgnoreCase(UUID organizationId, String registration);

	Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

	Optional<Employee> findByOrganizationIdAndRegistrationIgnoreCase(UUID organizationId, String registration);
}
