package dev.igorbarbosa.worktrainingsystem.employees.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.employees.api.CreateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeJobRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeStatusRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.UpdateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobService;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.application.SectorService;
import dev.igorbarbosa.worktrainingsystem.organizations.application.UnitService;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	private UUID unitId;
	private UUID sectorId;
	private UUID jobId;

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private UnitService unitService;

	@Mock
	private SectorService sectorService;

	@Mock
	private JobService jobService;

	@InjectMocks
	private EmployeeService employeeService;

	@BeforeEach
	void setUp() {
		unitId = UUID.randomUUID();
		sectorId = UUID.randomUUID();
		jobId = UUID.randomUUID();
	}

	@Test
	void createsEmployeeWithNormalizedValuesAndValidReferences() {
		stubActiveReferences(unitId);
		when(employeeRepository.saveAndFlush(any(Employee.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		employeeService.create(request(unitId, sectorId, jobId));

		ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
		verify(employeeRepository).saveAndFlush(captor.capture());
		Employee employee = captor.getValue();
		assertThat(employee.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
		assertThat(employee.getName()).isEqualTo("Ana Souza");
		assertThat(employee.getRegistration()).isEqualTo("100245");
		assertThat(employee.getEmail()).isEqualTo("ana.souza@empresa.com");
		assertThat(employee.getUnitId()).isEqualTo(unitId);
		assertThat(employee.getSectorId()).isEqualTo(sectorId);
		assertThat(employee.getJobId()).isEqualTo(jobId);
	}

	@Test
	void rejectsDuplicatedRegistrationIgnoringCase() {
		when(employeeRepository.existsByOrganizationIdAndRegistrationIgnoreCase(
				DEFAULT_ORGANIZATION_ID, "100245")).thenReturn(true);

		assertThatThrownBy(() -> employeeService.create(request(unitId, sectorId, jobId)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("REGISTRATION_ALREADY_EXISTS");
		verify(unitService, never()).getActive(any());
	}

	@Test
	void rejectsSectorFromAnotherUnit() {
		UUID actualSectorUnitId = UUID.randomUUID();
		when(unitService.getActive(unitId)).thenReturn(unit(unitId));
		when(sectorService.getActive(sectorId)).thenReturn(sector(sectorId, actualSectorUnitId));

		assertThatThrownBy(() -> employeeService.create(request(unitId, sectorId, jobId)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code")
				.isEqualTo("SECTOR_UNIT_MISMATCH");
		verify(jobService, never()).getActive(any());
		verify(employeeRepository, never()).saveAndFlush(any());
	}

	@Test
	void propagatesInactiveReferenceViolation() {
		when(unitService.getActive(unitId)).thenThrow(
				new BusinessRuleViolationException("UNIT_INACTIVE", "A unidade informada está inativa."));

		assertThatThrownBy(() -> employeeService.create(request(unitId, sectorId, jobId)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code")
				.isEqualTo("UNIT_INACTIVE");
		verify(employeeRepository, never()).saveAndFlush(any());
	}

	@Test
	void translatesDatabaseRegistrationConflict() {
		stubActiveReferences(unitId);
		when(employeeRepository.saveAndFlush(any(Employee.class)))
				.thenThrow(new DataIntegrityViolationException(
						"duplicate uk_employees_organization_registration"));

		assertThatThrownBy(() -> employeeService.create(request(unitId, sectorId, jobId)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("REGISTRATION_ALREADY_EXISTS");
	}

	@Test
	void updatesEmployeeAndRevalidatesOrganizationalStructure() {
		Employee employee = employee();
		UUID newUnitId = UUID.randomUUID();
		UUID newSectorId = UUID.randomUUID();
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));
		when(unitService.getActive(newUnitId)).thenReturn(unit(newUnitId));
		when(sectorService.getActive(newSectorId)).thenReturn(sector(newSectorId, newUnitId));
		stubReferenceMaps(newUnitId, newSectorId, jobId);

		employeeService.update(employee.getId(), new UpdateEmployeeRequest(
				"  Ana Lima  ", " 100246 ", " ANA.LIMA@EMPRESA.COM ", newSectorId, newUnitId));

		assertThat(employee.getName()).isEqualTo("Ana Lima");
		assertThat(employee.getRegistration()).isEqualTo("100246");
		assertThat(employee.getEmail()).isEqualTo("ana.lima@empresa.com");
		assertThat(employee.getUnitId()).isEqualTo(newUnitId);
		assertThat(employee.getSectorId()).isEqualTo(newSectorId);
		verify(employeeRepository).flush();
	}

	@Test
	void changesEmployeeStatus() {
		Employee employee = employee();
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));
		stubReferenceMaps(unitId, sectorId, jobId);

		employeeService.changeStatus(
				employee.getId(), new ChangeEmployeeStatusRequest(RegistrationStatus.INACTIVE));

		assertThat(employee.getStatus()).isEqualTo(RegistrationStatus.INACTIVE);
		verify(employeeRepository).flush();
	}

	@Test
	void changesJobForActiveEmployee() {
		Employee employee = employee();
		UUID newJobId = UUID.randomUUID();
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));
		when(jobService.getActive(newJobId)).thenReturn(job(newJobId));

		var response = employeeService.changeJob(
				employee.getId(), new ChangeEmployeeJobRequest(newJobId, false));

		assertThat(response.previousJobId()).isEqualTo(jobId);
		assertThat(response.currentJobId()).isEqualTo(newJobId);
		assertThat(response.activitiesAdded()).isZero();
		assertThat(employee.getJobId()).isEqualTo(newJobId);
	}

	@Test
	void rejectsJobChangeForInactiveEmployee() {
		Employee employee = employee();
		employee.changeStatus(RegistrationStatus.INACTIVE);
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));

		assertThatThrownBy(() -> employeeService.changeJob(
				employee.getId(), new ChangeEmployeeJobRequest(UUID.randomUUID(), false)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code")
				.isEqualTo("EMPLOYEE_INACTIVE");
		verify(jobService, never()).getActive(any());
	}

	@Test
	void rejectsDuplicatedRegistrationDuringUpdate() {
		Employee employee = employee();
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));
		when(employeeRepository.existsByOrganizationIdAndRegistrationIgnoreCase(
				DEFAULT_ORGANIZATION_ID, "100246")).thenReturn(true);

		assertThatThrownBy(() -> employeeService.update(
				employee.getId(), new UpdateEmployeeRequest(null, "100246", null, null, null)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("REGISTRATION_ALREADY_EXISTS");
		verify(employeeRepository, never()).flush();
	}

	@Test
	void rejectsUpdateWithoutChanges() {
		assertThatThrownBy(() -> employeeService.update(
				null, new UpdateEmployeeRequest(null, null, null, null, null)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code")
				.isEqualTo("NO_CHANGES");
		verify(employeeRepository, never()).findByIdAndOrganizationId(any(), any());
	}

	@Test
	void revalidatesReferencesBeforeReactivatingEmployee() {
		Employee employee = employee();
		employee.changeStatus(RegistrationStatus.INACTIVE);
		when(employeeRepository.findByIdAndOrganizationId(employee.getId(), DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(employee));
		when(unitService.getActive(unitId)).thenThrow(
				new BusinessRuleViolationException("UNIT_INACTIVE", "A unidade informada está inativa."));

		assertThatThrownBy(() -> employeeService.changeStatus(
				employee.getId(), new ChangeEmployeeStatusRequest(RegistrationStatus.ACTIVE)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code")
				.isEqualTo("UNIT_INACTIVE");
		assertThat(employee.getStatus()).isEqualTo(RegistrationStatus.INACTIVE);
	}

	private void stubActiveReferences(UUID sectorUnitId) {
		when(unitService.getActive(unitId)).thenReturn(unit(unitId));
		when(sectorService.getActive(sectorId)).thenReturn(sector(sectorId, sectorUnitId));
		when(jobService.getActive(jobId)).thenReturn(job(jobId));
	}

	private void stubReferenceMaps(UUID responseUnitId, UUID responseSectorId, UUID responseJobId) {
		when(unitService.getAllByIds(Set.of(responseUnitId)))
				.thenReturn(Map.of(responseUnitId, unit(responseUnitId)));
		when(sectorService.getAllByIds(Set.of(responseSectorId)))
				.thenReturn(Map.of(responseSectorId, sector(responseSectorId, responseUnitId)));
		when(jobService.getAllByIds(Set.of(responseJobId)))
				.thenReturn(Map.of(responseJobId, job(responseJobId)));
	}

	private Employee employee() {
		return new Employee(
				DEFAULT_ORGANIZATION_ID,
				"Ana Souza",
				"100245",
				"ana@empresa.com",
				jobId,
				sectorId,
				unitId,
				RegistrationStatus.ACTIVE);
	}

	private CreateEmployeeRequest request(UUID requestUnitId, UUID requestSectorId, UUID requestJobId) {
		return new CreateEmployeeRequest(
				"  Ana Souza  ",
				"  100245  ",
				"  ANA.SOUZA@EMPRESA.COM  ",
				requestJobId,
				requestSectorId,
				requestUnitId,
				RegistrationStatus.ACTIVE);
	}

	private UnitResponse unit(UUID id) {
		return new UnitResponse(id, "Unidade Centro", "CEN", RegistrationStatus.ACTIVE, null, null);
	}

	private SectorResponse sector(UUID id, UUID relatedUnitId) {
		return new SectorResponse(
				id, relatedUnitId, "Manutenção", "MAN", RegistrationStatus.ACTIVE, null, null);
	}

	private JobResponse job(UUID id) {
		return new JobResponse(id, "Operador", null, RegistrationStatus.ACTIVE, null, null);
	}
}
