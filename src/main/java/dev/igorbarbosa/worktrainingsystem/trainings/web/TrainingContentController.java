package dev.igorbarbosa.worktrainingsystem.trainings.web;

import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalogService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingContentController {

	private final TrainingCatalogService service;

	public TrainingContentController(TrainingCatalogService service) {
		this.service = service;
	}

	@PostMapping("/api/v1/modules/{moduleId}/videos")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<VideoResponse> createVideo(@PathVariable UUID moduleId,
			@Valid @RequestBody VideoRequest request) {
		VideoResponse response = service.createVideo(moduleId, request);
		return ResponseEntity.created(URI.create("/api/v1/videos/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/videos/{videoId}")
	@PreAuthorize("hasRole('ADMIN')")
	public VideoResponse getVideo(@PathVariable UUID videoId) {
		return service.getVideo(videoId);
	}

	@PatchMapping("/api/v1/videos/{videoId}")
	@PreAuthorize("hasRole('ADMIN')")
	public VideoResponse updateVideo(@PathVariable UUID videoId, @Valid @RequestBody VideoRequest request) {
		return service.updateVideo(videoId, request);
	}

	@PostMapping("/api/v1/modules/{moduleId}/questionnaire")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<QuestionnaireResponse> createQuestionnaire(@PathVariable UUID moduleId,
			@Valid @RequestBody QuestionnaireRequest request) {
		QuestionnaireResponse response = service.createQuestionnaire(moduleId, request);
		return ResponseEntity.created(URI.create("/api/v1/questionnaires/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/questionnaires/{questionnaireId}")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionnaireResponse getQuestionnaire(@PathVariable UUID questionnaireId) {
		return service.getQuestionnaire(questionnaireId);
	}

	@PatchMapping("/api/v1/questionnaires/{questionnaireId}")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionnaireResponse updateQuestionnaire(@PathVariable UUID questionnaireId,
			@Valid @RequestBody QuestionnaireRequest request) {
		return service.updateQuestionnaire(questionnaireId, request);
	}

	@PostMapping("/api/v1/questionnaires/{questionnaireId}/questions")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<QuestionResponse> createQuestion(@PathVariable UUID questionnaireId,
			@Valid @RequestBody QuestionRequest request) {
		QuestionResponse response = service.createQuestion(questionnaireId, request);
		return ResponseEntity.created(URI.create("/api/v1/questions/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/questionnaires/{questionnaireId}/questions")
	@PreAuthorize("hasRole('ADMIN')")
	public List<QuestionResponse> listQuestions(@PathVariable UUID questionnaireId) {
		return service.listQuestions(questionnaireId);
	}

	@PatchMapping("/api/v1/questions/{questionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionResponse updateQuestion(@PathVariable UUID questionId, @Valid @RequestBody QuestionRequest request) {
		return service.updateQuestion(questionId, request);
	}

	@GetMapping("/api/v1/questions/{questionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionResponse getQuestion(@PathVariable UUID questionId) {
		return service.getQuestion(questionId);
	}

	@PostMapping("/api/v1/questions/{questionId}/options")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AnswerOptionResponse> createAnswerOption(@PathVariable UUID questionId,
			@Valid @RequestBody AnswerOptionRequest request) {
		AnswerOptionResponse response = service.createAnswerOption(questionId, request);
		return ResponseEntity.created(URI.create("/api/v1/answer-options/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/questions/{questionId}/options")
	@PreAuthorize("hasRole('ADMIN')")
	public List<AnswerOptionResponse> listAnswerOptions(@PathVariable UUID questionId) {
		return service.listAnswerOptions(questionId);
	}

	@PatchMapping("/api/v1/answer-options/{optionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public AnswerOptionResponse updateAnswerOption(@PathVariable UUID optionId,
			@Valid @RequestBody AnswerOptionRequest request) {
		return service.updateAnswerOption(optionId, request);
	}

	@GetMapping("/api/v1/answer-options/{optionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public AnswerOptionResponse getAnswerOption(@PathVariable UUID optionId) {
		return service.getAnswerOption(optionId);
	}
}
