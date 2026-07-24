package dev.igorbarbosa.worktrainingsystem;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeEndpointTests {

	private final MockMvc mockMvc;
	private final EmployeeRepository employeeRepository;
	private final UnitRepository unitRepository;
	private final SectorRepository sectorRepository;
	private final JobRepository jobRepository;

	@Autowired
	EmployeeEndpointTests(
			MockMvc mockMvc,
			EmployeeRepository employeeRepository,
			UnitRepository unitRepository,
			SectorRepository sectorRepository,
			JobRepository jobRepository) {
		this.mockMvc = mockMvc;
		this.employeeRepository = employeeRepository;
		this.unitRepository = unitRepository;
		this.sectorRepository = sectorRepository;
		this.jobRepository = jobRepository;
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createsAndRetrievesEmployeeWithoutExposingJpaEntity() throws Exception {
		References references = activeReferences();

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody("100245", "ana.souza@empresa.com", references)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/employees/")))
				.andExpect(jsonPath("$.registration").value("100245"))
				.andExpect(jsonPath("$.job.name").value("Operador"))
				.andExpect(jsonPath("$.sector.name").value("Manutenção"))
				.andExpect(jsonPath("$.unit.name").value("Unidade Centro"))
				.andExpect(jsonPath("$.organizationId").doesNotExist())
				.andExpect(jsonPath("$.jobId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist());

		Employee employee = employeeRepository.findByOrganizationIdAndRegistrationIgnoreCase(
				DEFAULT_ORGANIZATION_ID, "100245").orElseThrow();

		mockMvc.perform(get("/api/v1/employees/{employeeId}", employee.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana.souza@empresa.com"));

		mockMvc.perform(get("/api/v1/employees/by-registration/{registration}", "100245"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(employee.getId().toString()));

		mockMvc.perform(get("/api/v1/employees")
					.param("search", "ana")
					.param("unitId", references.unit().getId().toString())
					.param("sort", "name,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].registration").value("100245"))
				.andExpect(jsonPath("$.sort[0].property").value("name"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsDuplicatedRegistration() throws Exception {
		References references = activeReferences();
		employeeRepository.saveAndFlush(new Employee(
				DEFAULT_ORGANIZATION_ID,
				"Ana Souza",
				"100245",
				"ana@empresa.com",
				references.job().getId(),
				references.sector().getId(),
				references.unit().getId(),
				RegistrationStatus.ACTIVE));

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody("100245", "outra@empresa.com", references)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_EXISTS"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void validatesEmailAndRequiredFields() throws Exception {
		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"","registration":"","email":"inválido","status":null}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsInactiveUnit() throws Exception {
		Unit inactiveUnit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Inativa", "INA", RegistrationStatus.INACTIVE));
		Unit activeUnit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Ativa", "ATI", RegistrationStatus.ACTIVE));
		Sector sector = sectorRepository.saveAndFlush(new Sector(
				DEFAULT_ORGANIZATION_ID, activeUnit, "Operação", "OPE", RegistrationStatus.ACTIVE));
		Job job = jobRepository.saveAndFlush(new Job(
				DEFAULT_ORGANIZATION_ID, "Operador", null, RegistrationStatus.ACTIVE));
		References references = new References(inactiveUnit, sector, job);

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody("100245", "ana@empresa.com", references)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("UNIT_INACTIVE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsInactiveSectorAndJob() throws Exception {
		Unit unit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Centro", "CEN", RegistrationStatus.ACTIVE));
		Sector inactiveSector = sectorRepository.saveAndFlush(new Sector(
				DEFAULT_ORGANIZATION_ID, unit, "Setor Inativo", "SIN", RegistrationStatus.INACTIVE));
		Sector activeSector = sectorRepository.saveAndFlush(new Sector(
				DEFAULT_ORGANIZATION_ID, unit, "Setor Ativo", "SAT", RegistrationStatus.ACTIVE));
		Job activeJob = jobRepository.saveAndFlush(new Job(
				DEFAULT_ORGANIZATION_ID, "Operador", null, RegistrationStatus.ACTIVE));
		Job inactiveJob = jobRepository.saveAndFlush(new Job(
				DEFAULT_ORGANIZATION_ID, "Cargo Inativo", null, RegistrationStatus.INACTIVE));

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody(
							"100245",
							"ana@empresa.com",
							new References(unit, inactiveSector, activeJob))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody(
							"100246",
							"bia@empresa.com",
							new References(unit, activeSector, inactiveJob))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("JOB_INACTIVE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsSectorFromAnotherUnit() throws Exception {
		References references = activeReferences();
		Unit anotherUnit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Norte", "NOR", RegistrationStatus.ACTIVE));
		References mismatched = new References(anotherUnit, references.sector(), references.job());

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody("100245", "ana@empresa.com", mismatched)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("SECTOR_UNIT_MISMATCH"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsMissingJob() throws Exception {
		References references = activeReferences();

		mockMvc.perform(post("/api/v1/employees")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBodyWithJobId(
							"100245", "ana@empresa.com", references, UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updatesProfileStatusAndJob() throws Exception {
		References references = activeReferences();
		Employee employee = employeeRepository.saveAndFlush(new Employee(
				DEFAULT_ORGANIZATION_ID,
				"Ana Souza",
				"100245",
				"ana@empresa.com",
				references.job().getId(),
				references.sector().getId(),
				references.unit().getId(),
				RegistrationStatus.ACTIVE));

		mockMvc.perform(patch("/api/v1/employees/{employeeId}", employee.getId())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Ana Lima","registration":"100246","email":"ANA.LIMA@EMPRESA.COM"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana Lima"))
				.andExpect(jsonPath("$.registration").value("100246"))
				.andExpect(jsonPath("$.email").value("ana.lima@empresa.com"));

		mockMvc.perform(patch("/api/v1/employees/{employeeId}/status", employee.getId())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"status":"INACTIVE"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));

		mockMvc.perform(patch("/api/v1/employees/{employeeId}/status", employee.getId())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"status":"ACTIVE"}
							"""))
				.andExpect(status().isOk());

		Job newJob = jobRepository.saveAndFlush(new Job(
				DEFAULT_ORGANIZATION_ID, "Supervisor", null, RegistrationStatus.ACTIVE));
		mockMvc.perform(patch("/api/v1/employees/{employeeId}/job", employee.getId())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"jobId":"%s","removePreviousJobActivities":false}
							""".formatted(newJob.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.previousJobId").value(references.job().getId().toString()))
				.andExpect(jsonPath("$.currentJobId").value(newJob.getId().toString()))
				.andExpect(jsonPath("$.activitiesAdded").value(0))
				.andExpect(jsonPath("$.assignmentsCreated").value(0));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsJobChangeForInactiveEmployee() throws Exception {
		References references = activeReferences();
		Employee employee = employeeRepository.saveAndFlush(new Employee(
				DEFAULT_ORGANIZATION_ID,
				"Ana Souza",
				"100245",
				"ana@empresa.com",
				references.job().getId(),
				references.sector().getId(),
				references.unit().getId(),
				RegistrationStatus.INACTIVE));

		mockMvc.perform(patch("/api/v1/employees/{employeeId}/job", employee.getId())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"jobId":"%s","removePreviousJobActivities":false}
							""".formatted(references.job().getId())))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("EMPLOYEE_INACTIVE"));
	}

	private References activeReferences() {
		Unit unit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Centro", "CEN", RegistrationStatus.ACTIVE));
		Sector sector = sectorRepository.saveAndFlush(new Sector(
				DEFAULT_ORGANIZATION_ID, unit, "Manutenção", "MAN", RegistrationStatus.ACTIVE));
		Job job = jobRepository.saveAndFlush(new Job(
				DEFAULT_ORGANIZATION_ID, "Operador", null, RegistrationStatus.ACTIVE));
		return new References(unit, sector, job);
	}

	private String requestBody(String registration, String email, References references) {
		return requestBodyWithJobId(registration, email, references, references.job().getId());
	}

	private String requestBodyWithJobId(
			String registration, String email, References references, UUID jobId) {
		return """
				{
				  "name":"Ana Souza",
				  "registration":"%s",
				  "email":"%s",
				  "jobId":"%s",
				  "sectorId":"%s",
				  "unitId":"%s",
				  "status":"ACTIVE"
				}
				""".formatted(
				registration,
				email,
				jobId,
				references.sector().getId(),
				references.unit().getId());
	}

	private record References(Unit unit, Sector sector, Job job) {
	}
}
