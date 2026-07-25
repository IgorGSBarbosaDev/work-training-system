package dev.igorbarbosa.worktrainingsystem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssignmentQualificationEndpointTests {
	@Autowired MockMvc mockMvc;

	@Test
	@WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "ADMIN")
	void exposesEmptyPagedOperationalListsAfterV8Migration() throws Exception {
		mockMvc.perform(get("/api/v1/training-assignments"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/v1/qualifications"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@WithMockUser(roles = "EMPLOYEE")
	void employeeCannotUseAdministrativeAssignmentList() throws Exception {
		mockMvc.perform(get("/api/v1/training-assignments")).andExpect(status().isForbidden());
	}
}
