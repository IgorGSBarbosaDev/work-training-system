package dev.igorbarbosa.worktrainingsystem.trainings.web;

import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.OrderRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;

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

	@PatchMapping("/api/v1/videos/{videoId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public VideoResponse videoStatus(@PathVariable UUID videoId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeVideoStatus(videoId, request.status());
	}

	@PatchMapping("/api/v1/modules/{moduleId}/videos/order")
	@PreAuthorize("hasRole('ADMIN')")
	public List<VideoResponse> reorderVideos(@PathVariable UUID moduleId, @Valid @RequestBody OrderRequest request) {
		return service.reorderVideos(moduleId, request);
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

	@PatchMapping("/api/v1/questionnaires/{questionnaireId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionnaireResponse questionnaireStatus(@PathVariable UUID questionnaireId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeQuestionnaireStatus(questionnaireId, request.status());
	}

	@DeleteMapping("/api/v1/modules/{moduleId}/questionnaire")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteQuestionnaire(@PathVariable UUID moduleId) {
		service.deleteQuestionnaire(moduleId); return ResponseEntity.noContent().build();
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

	@PatchMapping("/api/v1/questionnaires/{questionnaireId}/questions/order")
	@PreAuthorize("hasRole('ADMIN')")
	public List<QuestionResponse> reorderQuestions(@PathVariable UUID questionnaireId,
			@Valid @RequestBody OrderRequest request) { return service.reorderQuestions(questionnaireId, request); }

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

	@PatchMapping("/api/v1/questions/{questionId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public QuestionResponse questionStatus(@PathVariable UUID questionId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeQuestionStatus(questionId, request.status());
	}

	@DeleteMapping("/api/v1/questions/{questionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
		service.deleteQuestion(questionId); return ResponseEntity.noContent().build();
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

	@PatchMapping("/api/v1/questions/{questionId}/options/order")
	@PreAuthorize("hasRole('ADMIN')")
	public List<AnswerOptionResponse> reorderOptions(@PathVariable UUID questionId,
			@Valid @RequestBody OrderRequest request) { return service.reorderAnswerOptions(questionId, request); }

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

	@PatchMapping("/api/v1/answer-options/{optionId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public AnswerOptionResponse optionStatus(@PathVariable UUID optionId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeAnswerOptionStatus(optionId, request.status());
	}

	@DeleteMapping("/api/v1/answer-options/{optionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteOption(@PathVariable UUID optionId) {
		service.deleteAnswerOption(optionId); return ResponseEntity.noContent().build();
	}
}
