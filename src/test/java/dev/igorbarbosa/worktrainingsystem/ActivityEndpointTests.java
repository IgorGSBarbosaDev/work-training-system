package dev.igorbarbosa.worktrainingsystem;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityEndpointTests {
	@Autowired MockMvc mockMvc;

	@Test
	@WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "ADMIN")
	void createsListsUpdatesAndInactivatesActivity() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/activities").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Operar ponte rolante","description":"Operação industrial","status":"ACTIVE"}
						"""))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
		String id = field(created.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/api/v1/activities").param("sort", "name,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(patch("/api/v1/activities/{id}", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Atualizada\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Atualizada"));
		mockMvc.perform(patch("/api/v1/activities/{id}/status", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
	}

	private String field(String body, String name) {
		String marker = "\"" + name + "\":\""; int start = body.indexOf(marker) + marker.length();
		return body.substring(start, body.indexOf('"', start));
	}
}
