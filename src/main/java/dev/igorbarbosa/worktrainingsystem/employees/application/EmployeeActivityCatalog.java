package dev.igorbarbosa.worktrainingsystem.employees.application;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public employee boundary used by activity propagation. */
public interface EmployeeActivityCatalog {
	EmployeeSummary requireEmployee(UUID employeeId);
	EmployeeSummary requireActiveEmployee(UUID employeeId);
	List<UUID> findActiveEmployeeIdsByJob(UUID jobId);
	Page<EmployeeSummary> findActiveByJob(UUID jobId, Collection<UUID> allowedEmployeeIds, Pageable pageable);
	Page<EmployeeSummary> findActiveBySector(UUID sectorId, Collection<UUID> allowedEmployeeIds, Pageable pageable);
	Page<EmployeeSummary> findActiveByUnit(UUID unitId, Collection<UUID> allowedEmployeeIds, Pageable pageable);
	Page<EmployeeSummary> findActiveByIds(Collection<UUID> employeeIds, Pageable pageable);
	Map<UUID, EmployeeSummary> summaries(Collection<UUID> employeeIds);

	record EmployeeSummary(UUID id, String name, String registration, UUID jobId, UUID sectorId,
			UUID unitId, boolean active) {}
}
