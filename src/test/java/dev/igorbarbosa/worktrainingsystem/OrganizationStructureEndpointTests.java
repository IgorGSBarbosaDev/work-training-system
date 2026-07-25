package dev.igorbarbosa.worktrainingsystem;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
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
class OrganizationStructureEndpointTests {

	private final MockMvc mockMvc;
	private final UnitRepository unitRepository;

	@Autowired
	OrganizationStructureEndpointTests(MockMvc mockMvc, UnitRepository unitRepository) {
		this.mockMvc = mockMvc;
		this.unitRepository = unitRepository;
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createsAndListsUnitsWithPaginationAndSearch() throws Exception {
		mockMvc.perform(post("/api/v1/units")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Unidade Norte","code":"nor","status":"ACTIVE"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/units/")))
				.andExpect(jsonPath("$.name").value("Unidade Norte"))
				.andExpect(jsonPath("$.code").value("NOR"));

		mockMvc.perform(get("/api/v1/units")
					.param("page", "0")
					.param("size", "1")
					.param("sort", "name,asc")
					.param("search", "norte"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.sort[0].property").value("name"))
				.andExpect(jsonPath("$.sort[0].direction").value("ASC"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void returnsConflictForDuplicatedUnit() throws Exception {
		unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Sul", "SUL", RegistrationStatus.ACTIVE));

		mockMvc.perform(post("/api/v1/units")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"unidade sul","code":"OUTRO","status":"ACTIVE"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("UNIT_ALREADY_EXISTS"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void validatesUnitPayloadAndPagination() throws Exception {
		mockMvc.perform(post("/api/v1/units")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":" ","code":"código inválido","status":null}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(3)));

		mockMvc.perform(get("/api/v1/units").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createsAndListsSectorsForAUnit() throws Exception {
		Unit unit = unitRepository.saveAndFlush(new Unit(
				DEFAULT_ORGANIZATION_ID, "Unidade Centro", "CEN", RegistrationStatus.ACTIVE));

		mockMvc.perform(post("/api/v1/sectors")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"unitId":"%s","name":"Manutenção","code":"man","status":"ACTIVE"}
							""".formatted(unit.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.unitId").value(unit.getId().toString()))
				.andExpect(jsonPath("$.code").value("MAN"));

		mockMvc.perform(get("/api/v1/sectors").param("unitId", unit.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].name").value("Manutenção"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void returnsNotFoundWhenSectorUnitDoesNotExist() throws Exception {
		mockMvc.perform(post("/api/v1/sectors")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"unitId":"c69a0cb9-23b0-43f9-80ba-c4e676e3ab01","name":"TI","code":"TI","status":"ACTIVE"}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createsAndListsJobs() throws Exception {
		mockMvc.perform(post("/api/v1/jobs")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Operador Industrial","description":"Opera máquinas.","status":"ACTIVE"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Operador Industrial"));

		mockMvc.perform(get("/api/v1/jobs").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].description").value("Opera máquinas."));
	}

	@Test
	@WithMockUser(roles = "EMPLOYEE")
	void rejectsCreationWithoutAdministratorRole() throws Exception {
		mockMvc.perform(post("/api/v1/jobs")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name":"Operador","status":"ACTIVE"}
							"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void readsAndUpdatesOrganizationSettingsWithoutWeakeningMvpThresholds() throws Exception {
		mockMvc.perform(get("/api/v1/organization/settings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expiringSoonDays").value(30))
				.andExpect(jsonPath("$.defaultPassingScore").value(70))
				.andExpect(jsonPath("$.defaultRequiredVideoPercentage").value(80));

		mockMvc.perform(patch("/api/v1/organization/settings").with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"expiringSoonDays\":45,\"defaultPassingScore\":75,\"defaultRequiredVideoPercentage\":80}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expiringSoonDays").value(45))
				.andExpect(jsonPath("$.defaultPassingScore").value(75));

		mockMvc.perform(patch("/api/v1/organization/settings").with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"defaultPassingScore\":69,\"defaultRequiredVideoPercentage\":79}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getsUpdatesAndChangesUnitStatus() throws Exception {
		Unit unit = unitRepository.saveAndFlush(new Unit(DEFAULT_ORGANIZATION_ID,
				"Unidade Centro", "CEN", RegistrationStatus.ACTIVE));

		mockMvc.perform(get("/api/v1/units/{id}", unit.getId()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.code").value("CEN"));
		mockMvc.perform(patch("/api/v1/units/{id}", unit.getId()).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Unidade Central\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Unidade Central"));
		mockMvc.perform(patch("/api/v1/units/{id}/status", unit.getId()).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
	}
}
