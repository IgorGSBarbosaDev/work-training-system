package dev.igorbarbosa.worktrainingsystem;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.uploads.allow-legacy-object-keys=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrainingCatalogEndpointTests {

	private final MockMvc mockMvc;

	@Autowired
	TrainingCatalogEndpointTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createsTrainingContentAndPublishesVersion() throws Exception {
		MvcResult trainingResult = mockMvc.perform(post("/api/v1/trainings")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name":"NR-11",
							  "code":"nr11",
							  "description":"Movimentação e armazenagem.",
							  "category":"Normas Regulamentadoras",
							  "isRegulatoryStandard":true,
							  "status":"ACTIVE",
							  "initialVersion":{
							    "workloadMinutes":480,
							    "validityType":"MONTHS",
							    "validityValue":24,
							    "passingScore":70,
							    "maxAttempts":3,
							    "retryIntervalMinutes":1440
							  }
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("NR11"))
				.andReturn();
		UUID trainingId = idFrom(trainingResult);

		MvcResult versionsResult = mockMvc.perform(post("/api/v1/trainings/{trainingId}/versions", trainingId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(versionPayload()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.versionNumber").value(2))
				.andReturn();
		UUID versionId = idFrom(versionsResult);

		MvcResult moduleResult = mockMvc.perform(post("/api/v1/training-versions/{versionId}/modules", versionId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"title":"Fundamentos","description":"Conceitos iniciais.","order":1,"status":"ACTIVE"}
							"""))
				.andExpect(status().isCreated())
				.andReturn();
		UUID moduleId = idFrom(moduleResult);

		MvcResult videoResult = mockMvc.perform(post("/api/v1/modules/{moduleId}/videos", moduleId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"title":"Introdução","description":"Visão geral.","order":1,"durationSeconds":900,"storageObjectKey":"videos/nr11/v2/intro.mp4","required":true,"status":"ACTIVE"}
							"""))
					.andExpect(status().isCreated())
					.andReturn();
		UUID videoId = idFrom(videoResult);

		mockMvc.perform(get("/api/v1/modules/{moduleId}/videos", moduleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(videoId.toString()));

		MvcResult questionnaireResult = mockMvc.perform(post("/api/v1/modules/{moduleId}/questionnaire", moduleId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"title":"Avaliação","passingScore":70,"maxAttempts":3,"retryIntervalMinutes":0,"shuffleQuestions":false,"status":"ACTIVE"}
							"""))
					.andExpect(status().isCreated())
					.andReturn();
		UUID questionnaireId = idFrom(questionnaireResult);

		mockMvc.perform(get("/api/v1/modules/{moduleId}/questionnaire", moduleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(questionnaireId.toString()));

		MvcResult questionResult = mockMvc.perform(post("/api/v1/questionnaires/{questionnaireId}/questions", questionnaireId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"statement":"Qual é o mínimo?","order":1,"status":"ACTIVE"}
							"""))
				.andExpect(status().isCreated())
				.andReturn();
		UUID questionId = idFrom(questionResult);

		createOption(questionId, "50%", false, 1);
		createOption(questionId, "80%", true, 2);

		mockMvc.perform(post("/api/v1/training-versions/{versionId}/publish", versionId).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));

		mockMvc.perform(patch("/api/v1/training-versions/{versionId}", versionId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(versionPayload()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("PUBLISHED_CONTENT_IMMUTABLE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void removesDraftModuleWithNestedContent() throws Exception {
		MvcResult trainingResult = mockMvc.perform(post("/api/v1/trainings")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(trainingPayload()))
				.andExpect(status().isCreated())
				.andReturn();
		UUID trainingId = idFrom(trainingResult);
		MvcResult versionResult = mockMvc.perform(post("/api/v1/trainings/{trainingId}/versions", trainingId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(versionPayload()))
				.andExpect(status().isCreated())
				.andReturn();
		UUID versionId = idFrom(versionResult);
		MvcResult moduleResult = mockMvc.perform(post("/api/v1/training-versions/{versionId}/modules", versionId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Remover\",\"description\":null,\"order\":1,\"status\":\"ACTIVE\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		UUID moduleId = idFrom(moduleResult);

		mockMvc.perform(post("/api/v1/modules/{moduleId}/videos", moduleId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Vídeo\",\"description\":null,\"order\":1,\"durationSeconds\":60,\"storageObjectKey\":\"videos/remove.mp4\",\"required\":true,\"status\":\"ACTIVE\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/modules/{moduleId}/questionnaire", moduleId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Questionário\",\"passingScore\":70,\"maxAttempts\":1,\"retryIntervalMinutes\":0,\"shuffleQuestions\":false,\"status\":\"ACTIVE\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/modules/{moduleId}", moduleId).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/modules/{moduleId}", moduleId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsPublishingVersionWithoutContent() throws Exception {
		MvcResult trainingResult = mockMvc.perform(post("/api/v1/trainings")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(trainingPayload()))
				.andExpect(status().isCreated())
				.andReturn();
		UUID trainingId = idFrom(trainingResult);
		MvcResult versionResult = mockMvc.perform(post("/api/v1/trainings/{trainingId}/versions", trainingId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(versionPayload()))
				.andExpect(status().isCreated())
				.andReturn();

		mockMvc.perform(post("/api/v1/training-versions/{versionId}/publish", idFrom(versionResult)).with(csrf()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("VERSION_CONTENT_INVALID"));
	}

	private void createOption(UUID questionId, String text, boolean correct, int order) throws Exception {
		mockMvc.perform(post("/api/v1/questions/{questionId}/options", questionId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"text":"%s","correct":%s,"order":%d,"status":"ACTIVE"}
							""".formatted(text, correct, order)))
				.andExpect(status().isCreated());
	}

	private UUID idFrom(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		String marker = "\"id\":\"";
		int start = body.indexOf(marker);
		if (start < 0) {
			throw new AssertionError("Response does not contain an id: " + body);
		}
		start += marker.length();
		return UUID.fromString(body.substring(start, body.indexOf('"', start)));
	}

	private String trainingPayload() {
		return """
				{"name":"Segurança Operacional","code":"SEG-01","isRegulatoryStandard":false,"status":"ACTIVE","initialVersion":{"workloadMinutes":60,"validityType":"INDEFINITE","passingScore":70,"retryIntervalMinutes":0}}
				""";
	}

	private String versionPayload() {
		return """
				{"workloadMinutes":480,"validityType":"MONTHS","validityValue":24,"passingScore":70,"maxAttempts":3,"retryIntervalMinutes":1440}
				""";
	}
}
