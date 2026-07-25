package dev.igorbarbosa.worktrainingsystem.employees.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class EmployeeActivityCatalogAdapter implements EmployeeActivityCatalog {
	private final EmployeeRepository employees;
	EmployeeActivityCatalogAdapter(EmployeeRepository employees) { this.employees = employees; }

	@Override @Transactional(readOnly = true)
	public EmployeeSummary requireEmployee(UUID employeeId) { return summary(find(employeeId)); }

	@Override @Transactional(readOnly = true)
	public EmployeeSummary requireActiveEmployee(UUID employeeId) {
		Employee employee = find(employeeId);
		if (employee.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException("EMPLOYEE_INACTIVE", "Colaborador inativo não pode receber atividades.");
		}
		return summary(employee);
	}

	@Override @Transactional(readOnly = true)
	public List<UUID> findActiveEmployeeIdsByJob(UUID jobId) {
		return employees.findIdsByOrganizationIdAndJobIdAndStatus(
				DEFAULT_ORGANIZATION_ID, jobId, RegistrationStatus.ACTIVE);
	}

	@Override @Transactional(readOnly = true)
	public Page<EmployeeSummary> findActiveByJob(UUID jobId, Collection<UUID> allowedEmployeeIds, Pageable pageable) {
		return activeBy("jobId", jobId, allowedEmployeeIds, pageable);
	}
	@Override @Transactional(readOnly = true)
	public Page<EmployeeSummary> findActiveBySector(UUID sectorId, Collection<UUID> allowedEmployeeIds, Pageable pageable) {
		return activeBy("sectorId", sectorId, allowedEmployeeIds, pageable);
	}
	@Override @Transactional(readOnly = true)
	public Page<EmployeeSummary> findActiveByUnit(UUID unitId, Collection<UUID> allowedEmployeeIds, Pageable pageable) {
		return activeBy("unitId", unitId, allowedEmployeeIds, pageable);
	}
	@Override @Transactional(readOnly = true)
	public Page<EmployeeSummary> findActiveByIds(Collection<UUID> employeeIds, Pageable pageable) {
		if (employeeIds.isEmpty()) return Page.empty(pageable);
		return activeBy(null, null, employeeIds, pageable);
	}
	@Override @Transactional(readOnly = true)
	public Map<UUID, EmployeeSummary> summaries(Collection<UUID> employeeIds) {
		return employees.findAllById(employeeIds).stream()
				.filter(item -> item.getOrganizationId().equals(DEFAULT_ORGANIZATION_ID))
				.collect(Collectors.toUnmodifiableMap(Employee::getId, this::summary));
	}

	private Page<EmployeeSummary> activeBy(String property, UUID value, Collection<UUID> ids, Pageable pageable) {
		Specification<Employee> specification = (root, query, cb) -> cb.and(
				cb.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID),
				cb.equal(root.get("status"), RegistrationStatus.ACTIVE));
		if (property != null) specification = specification.and((root, query, cb) -> cb.equal(root.get(property), value));
		if (ids != null) specification = ids.isEmpty()
				? specification.and((root, query, cb) -> cb.disjunction())
				: specification.and((root, query, cb) -> root.get("id").in(ids));
		return employees.findAll(specification, pageable).map(this::summary);
	}

	private Employee find(UUID id) {
		return employees.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O colaborador informado não existe."));
	}
	private EmployeeSummary summary(Employee employee) {
		return new EmployeeSummary(employee.getId(), employee.getName(), employee.getRegistration(), employee.getJobId(),
				employee.getSectorId(), employee.getUnitId(), employee.getStatus() == RegistrationStatus.ACTIVE);
	}
}
