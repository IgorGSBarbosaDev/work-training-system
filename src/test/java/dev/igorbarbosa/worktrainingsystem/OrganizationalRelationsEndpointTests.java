package dev.igorbarbosa.worktrainingsystem;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Training;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingVersionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrganizationalRelationsEndpointTests {

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final UnitRepository units;
	private final SectorRepository sectors;
	private final JobRepository jobs;
	private final EmployeeRepository employees;
	private final TrainingRepository trainings;
	private final TrainingVersionRepository versions;
	private final UserRepository users;

	@Autowired
	OrganizationalRelationsEndpointTests(MockMvc mockMvc, ObjectMapper objectMapper, UnitRepository units,
			SectorRepository sectors, JobRepository jobs, EmployeeRepository employees, TrainingRepository trainings,
			TrainingVersionRepository versions, UserRepository users) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.units = units;
		this.sectors = sectors;
		this.jobs = jobs;
		this.employees = employees;
		this.trainings = trainings;
		this.versions = versions;
		this.users = users;
	}

	@Test
	void propagatesRelationsAndRecalculatesQualificationStatesWithoutDeletingHistory() throws Exception {
		Instant completionAt = Instant.now().minus(5, ChronoUnit.DAYS);
		User actor = users.saveAndFlush(new User(DEFAULT_ORGANIZATION_ID, "priority2-admin@example.com", "hash",
				UserRole.ADMIN, UserStatus.ACTIVE, null, Instant.parse("2026-07-29T12:00:00Z")));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				actor.getId().toString(), "test", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
		Unit unit = units.saveAndFlush(new Unit(DEFAULT_ORGANIZATION_ID, "Unidade Prioridade 2", "P2", RegistrationStatus.ACTIVE));
		Sector sector = sectors.saveAndFlush(new Sector(DEFAULT_ORGANIZATION_ID, unit, "Operação P2", "OP2", RegistrationStatus.ACTIVE));
		Job firstJob = jobs.saveAndFlush(new Job(DEFAULT_ORGANIZATION_ID, "Operador P2", "Cargo inicial", RegistrationStatus.ACTIVE));
		Job secondJob = jobs.saveAndFlush(new Job(DEFAULT_ORGANIZATION_ID, "Técnico P2", "Cargo novo", RegistrationStatus.ACTIVE));
		Training training = trainings.saveAndFlush(new Training(DEFAULT_ORGANIZATION_ID, "Treinamento P2", "P2-TR",
				"Requisito do fluxo", "Operacional", false, RegistrationStatus.ACTIVE));
		TrainingVersion version = new TrainingVersion(training, 1, 60, ValidityType.DAYS, 10,
				BigDecimal.valueOf(70), 3, 0);
		version.capturePublicationSnapshot(training, "{\"modules\":[]}");
		version.publish(Instant.parse("2026-07-29T12:00:00Z"));
		versions.saveAndFlush(version);
		Training trainingTwo = trainings.saveAndFlush(new Training(DEFAULT_ORGANIZATION_ID, "Treinamento P2 2", "P2-TR2",
				"Segundo requisito do fluxo", "Operacional", false, RegistrationStatus.ACTIVE));
		TrainingVersion versionTwo = new TrainingVersion(trainingTwo, 1, 60, ValidityType.DAYS, 10,
				BigDecimal.valueOf(70), 3, 0);
		versionTwo.capturePublicationSnapshot(trainingTwo, "{\"modules\":[]}");
		versionTwo.publish(Instant.parse("2026-07-29T12:00:00Z"));
		versions.saveAndFlush(versionTwo);

		UUID activityOne = createActivity("Operar ponte P2");
		UUID activityTwo = createActivity("Operar empilhadeira P2");
		UUID activityThree = createActivity("Atividade específica P2");
		UUID activityFour = createActivity("Atividade sem requisito P2");
		Employee employee = employees.saveAndFlush(new Employee(DEFAULT_ORGANIZATION_ID, "Colaborador P2", "P2-001",
				"p2-001@example.com", firstJob.getId(), sector.getId(), unit.getId(), RegistrationStatus.ACTIVE));

		linkJob(firstJob.getId(), activityOne);
		linkJob(secondJob.getId(), activityTwo);
		linkJob(secondJob.getId(), activityFour);
		addRequirement(activityOne, training.getId());
		addRequirement(activityTwo, trainingTwo.getId());

		mockMvc.perform(get("/api/v1/employees/{employeeId}/activities", employee.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].activity.id").value(activityOne.toString()))
				.andExpect(jsonPath("$[0].origins", hasItem("JOB")));
		mockMvc.perform(get("/api/v1/training-assignments").param("employeeId", employee.getId().toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/v1/qualifications").param("employeeId", employee.getId().toString())
					.param("activityId", activityOne.toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].status").value("BLOCKED"));

		MvcResult jobChange = mockMvc.perform(patch("/api/v1/employees/{employeeId}/job", employee.getId()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jobId\":\"%s\",\"removePreviousJobActivities\":false}".formatted(secondJob.getId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.activitiesAdded").value(2))
				.andExpect(jsonPath("$.assignmentsCreated").value(1)).andReturn();
		org.assertj.core.api.Assertions.assertThat(jobChange.getResponse().getContentAsString()).contains(secondJob.getId().toString());

		mockMvc.perform(get("/api/v1/training-assignments").param("employeeId", employee.getId().toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
		mockMvc.perform(get("/api/v1/qualifications").param("employeeId", employee.getId().toString())
					.param("activityId", activityFour.toString()).param("status", "AVAILABLE"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/v1/employees/{employeeId}/activities", employee.getId()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"activityId\":\"%s\",\"reason\":\"Autorização P2\"}".formatted(activityThree)))
				.andExpect(status().isCreated());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
					"/api/v1/employees/{employeeId}/activities/{activityId}", employee.getId(), activityThree).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/qualifications").param("employeeId", employee.getId().toString())
					.param("activityId", activityThree.toString()).param("status", "NOT_ASSIGNED"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/v1/training-completions/manual").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"employeeId\":\"%s\",\"trainingId\":\"%s\",\"trainingVersionId\":\"%s\",\"completedAt\":\"%s\",\"score\":100,\"validityType\":\"DAYS\",\"validityValue\":10,\"notes\":\"Conclusão externa P2\"}"
						.formatted(employee.getId(), training.getId(), version.getId(), completionAt)))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/v1/qualifications").param("employeeId", employee.getId().toString())
					.param("activityId", activityOne.toString()).param("status", "EXPIRING"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/v1/employees/{employeeId}/completions", employee.getId()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
	}

	private UUID createActivity(String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/activities").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"%s\",\"description\":\"Fluxo P2\",\"status\":\"ACTIVE\"}".formatted(name)))
				.andExpect(status().isCreated()).andReturn();
		return idFrom(result);
	}

	private void addRequirement(UUID activityId, UUID trainingId) throws Exception {
		mockMvc.perform(post("/api/v1/activities/{activityId}/requirements", activityId).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"trainingId\":\"%s\",\"versionPolicy\":\"LATEST_PUBLISHED\",\"required\":true,\"applyToCurrentEmployees\":true}"
						.formatted(trainingId)))
				.andExpect(status().isCreated());
	}

	private void linkJob(UUID jobId, UUID activityId) throws Exception {
		mockMvc.perform(post("/api/v1/jobs/{jobId}/activities", jobId).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"activityId\":\"%s\",\"applyToCurrentEmployees\":true}".formatted(activityId)))
				.andExpect(status().isCreated());
	}

	private UUID idFrom(MvcResult result) throws Exception {
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(body.get("id").asText());
	}
}
