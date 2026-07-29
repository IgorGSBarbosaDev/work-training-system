package dev.igorbarbosa.worktrainingsystem;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.identity.domain.AccessScopeGrant;
import dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AccessScopeGrantRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.PresignedObjectUrl;
import java.util.UUID;
import java.time.Instant;
import java.time.Duration;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeEndpointTests {
	@MockitoBean
	private ObjectStorage objectStorage;

	private final MockMvc mockMvc;
	private final EmployeeRepository employeeRepository;
	private final UnitRepository unitRepository;
	private final SectorRepository sectorRepository;
	private final JobRepository jobRepository;
	private final EmployeeHistoryRepository historyRepository;
	private final UserRepository userRepository;
	private final AccessScopeGrantRepository scopeRepository;

	@Autowired
	EmployeeEndpointTests(
			MockMvc mockMvc,
			EmployeeRepository employeeRepository,
			UnitRepository unitRepository,
			SectorRepository sectorRepository,
			JobRepository jobRepository,
			EmployeeHistoryRepository historyRepository,
			UserRepository userRepository,
			AccessScopeGrantRepository scopeRepository) {
		this.mockMvc = mockMvc;
		this.employeeRepository = employeeRepository;
		this.unitRepository = unitRepository;
		this.sectorRepository = sectorRepository;
		this.jobRepository = jobRepository;
		this.historyRepository = historyRepository;
		this.userRepository = userRepository;
		this.scopeRepository = scopeRepository;
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
	void rejectsDuplicatedEmployeeEmailIgnoringCase() throws Exception {
		References references = activeReferences();
		employeeRepository.saveAndFlush(new Employee(DEFAULT_ORGANIZATION_ID, "Ana Souza", "100245",
				"ana@empresa.com", references.job().getId(), references.sector().getId(), references.unit().getId(),
				RegistrationStatus.ACTIVE));

		mockMvc.perform(post("/api/v1/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(requestBody("100246", "ANA@EMPRESA.COM", references)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
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

		mockMvc.perform(get("/api/v1/employees/{employeeId}/history", employee.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(4));
		org.assertj.core.api.Assertions.assertThat(historyRepository.findAllByEmployeeId(
				employee.getId(), org.springframework.data.domain.Pageable.unpaged())).hasSize(4);
	}

	@Test
	void managerScopeFiltersAtTheRepositoryAndBlocksCrossScopeDetails() throws Exception {
		References allowed = activeReferences();
		Unit otherUnit = unitRepository.saveAndFlush(new Unit(DEFAULT_ORGANIZATION_ID,
				"Unidade Oeste", "OES", RegistrationStatus.ACTIVE));
		Sector otherSector = sectorRepository.saveAndFlush(new Sector(DEFAULT_ORGANIZATION_ID,
				otherUnit, "Logística", "LOG", RegistrationStatus.ACTIVE));
		Job otherJob = jobRepository.saveAndFlush(new Job(DEFAULT_ORGANIZATION_ID,
				"Logístico", null, RegistrationStatus.ACTIVE));
		Employee inScope = employeeRepository.saveAndFlush(new Employee(DEFAULT_ORGANIZATION_ID, "Ana", "100245",
				"ana@empresa.com", allowed.job().getId(), allowed.sector().getId(), allowed.unit().getId(),
				RegistrationStatus.ACTIVE));
		Employee outside = employeeRepository.saveAndFlush(new Employee(DEFAULT_ORGANIZATION_ID, "Bia", "100246",
				"bia@empresa.com", otherJob.getId(), otherSector.getId(), otherUnit.getId(), RegistrationStatus.ACTIVE));
		User manager = userRepository.saveAndFlush(new User(DEFAULT_ORGANIZATION_ID, "manager@empresa.com", "hash",
				UserRole.MANAGER, UserStatus.ACTIVE, null, Instant.now()));
		scopeRepository.saveAndFlush(new AccessScopeGrant(manager.getId(), DEFAULT_ORGANIZATION_ID,
				ScopeType.UNIT, allowed.unit().getId()));

		var managerJwt = jwt().jwt(token -> token.subject(manager.getId().toString())
				.claim("org", DEFAULT_ORGANIZATION_ID.toString()).claim("role", "MANAGER")
				.claim("permissions", List.of()))
				.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
		mockMvc.perform(get("/api/v1/employees").with(managerJwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(inScope.getId().toString()));
		mockMvc.perform(get("/api/v1/employees/{id}", outside.getId()).with(managerJwt))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/sectors/{id}", otherSector.getId()).with(managerJwt))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/jobs/{id}", allowed.job().getId()).with(managerJwt))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/jobs/{id}", otherJob.getId()).with(managerJwt))
				.andExpect(status().isForbidden());
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

	@Test
	@WithMockUser(roles = "ADMIN")
	void validatesAndStoresEmployeePhotoWithTemporaryUrl() throws Exception {
		References references = activeReferences();
		Employee employee = employeeRepository.saveAndFlush(new Employee(DEFAULT_ORGANIZATION_ID, "Ana", "100245",
				"ana@empresa.com", references.job().getId(), references.sector().getId(), references.unit().getId(),
				RegistrationStatus.ACTIVE));
		byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
		org.mockito.Mockito.when(objectStorage.presignDownload(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(new PresignedObjectUrl(URI.create("https://storage.example/temporary-photo"),
						Instant.now().plus(Duration.ofMinutes(15))));

		mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/employees/{id}/photo", employee.getId())
					.file(new MockMultipartFile("file", "unsafe.png", "image/png", png)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photoUrl").value("https://storage.example/temporary-photo"))
				.andExpect(jsonPath("$.photoObjectKey").doesNotExist());

		mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/employees/{id}/photo", employee.getId())
					.file(new MockMultipartFile("file", "fake.png", "image/png", "not-image".getBytes())))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("PHOTO_CONTENT_INVALID"));
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
