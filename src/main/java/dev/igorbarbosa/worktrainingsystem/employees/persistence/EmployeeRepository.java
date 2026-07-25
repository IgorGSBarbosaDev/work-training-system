package dev.igorbarbosa.worktrainingsystem.employees.persistence;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

	boolean existsByOrganizationIdAndRegistrationIgnoreCase(UUID organizationId, String registration);

	Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

	Optional<Employee> findByOrganizationIdAndRegistrationIgnoreCase(UUID organizationId, String registration);

	boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

	@Query("""
			select e.id from Employee e where e.organizationId = :organizationId
			and e.jobId = :jobId and e.status = :status order by e.id
			""")
	List<UUID> findIdsByOrganizationIdAndJobIdAndStatus(UUID organizationId, UUID jobId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status);

	@Query("""
			select e.id as id, e.organizationId as organizationId, e.unitId as unitId,
			       e.sectorId as sectorId, e.jobId as jobId
			from Employee e
			where e.organizationId = :organizationId
			and e.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			and (e.unitId in :unitIds or e.sectorId in :sectorIds or e.id in :employeeIds)
			""")
	List<Employee> findActiveInScope(UUID organizationId, Set<UUID> unitIds, Set<UUID> sectorIds,
			Set<UUID> employeeIds);
}
