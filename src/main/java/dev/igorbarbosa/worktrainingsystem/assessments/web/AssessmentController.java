package dev.igorbarbosa.worktrainingsystem.assessments.web;

import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptDetailResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptSummaryResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAvailabilityResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.QuestionnaireDeliveryResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.application.AssessmentService;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class AssessmentController {
	private static final Set<String> SORTABLE = Set.of("submittedAt", "attemptNumber", "score", "result");
	private final AssessmentService service;
	private final PaginationFactory pagination;
	public AssessmentController(AssessmentService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}

	@GetMapping("/training-assignments/{assignmentId}/questionnaires/{questionnaireId}")
	@PreAuthorize("hasRole('EMPLOYEE')")
	public QuestionnaireDeliveryResponse questionnaire(@PathVariable UUID assignmentId, @PathVariable UUID questionnaireId) {
		return service.questionnaire(assignmentId, questionnaireId);
	}

	@GetMapping("/training-assignments/{assignmentId}/questionnaires/{questionnaireId}/availability")
	@PreAuthorize("hasRole('EMPLOYEE')")
	public AssessmentAvailabilityResponse availability(@PathVariable UUID assignmentId, @PathVariable UUID questionnaireId) {
		return service.availability(assignmentId, questionnaireId);
	}

	@PostMapping("/training-assignments/{assignmentId}/questionnaires/{questionnaireId}/attempts")
	@PreAuthorize("hasRole('EMPLOYEE')")
	public ResponseEntity<AssessmentAttemptResponse> submit(@PathVariable UUID assignmentId,
			@PathVariable UUID questionnaireId, @Valid @RequestBody AssessmentAttemptRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		AssessmentAttemptResponse response = service.submit(assignmentId, questionnaireId, request, idempotencyKey);
		return ResponseEntity.created(URI.create("/api/v1/assessment-attempts/" + response.attemptId())).body(response);
	}

	@GetMapping("/training-assignments/{assignmentId}/assessment-attempts")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public PageResponse<AssessmentAttemptSummaryResponse> history(@PathVariable UUID assignmentId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "submittedAt,desc") String sort) {
		return PageResponse.from(service.history(assignmentId, pagination.create(page, size, sort, SORTABLE)));
	}

	@GetMapping("/assessment-attempts/{attemptId}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public AssessmentAttemptDetailResponse detail(@PathVariable UUID attemptId) { return service.detail(attemptId); }
}
