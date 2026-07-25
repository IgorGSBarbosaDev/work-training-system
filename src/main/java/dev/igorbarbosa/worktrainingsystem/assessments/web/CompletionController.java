package dev.igorbarbosa.worktrainingsystem.assessments.web;

import dev.igorbarbosa.worktrainingsystem.assessments.api.CompletionResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.ManualCompletionRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.application.CompletionService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CompletionController {
	private static final Set<String> SORTABLE = Set.of("completedAt", "completionDate", "expirationDate", "finalScore", "createdAt");
	private final CompletionService service;
	private final PaginationFactory pagination;
	public CompletionController(CompletionService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}

	@PostMapping("/training-completions/manual") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CompletionResponse> manual(@Valid @RequestBody ManualCompletionRequest request) {
		CompletionResponse response = service.manual(request);
		return ResponseEntity.created(URI.create("/api/v1/training-completions/" + response.id())).body(response);
	}

	@GetMapping("/training-completions") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<CompletionResponse> list(@RequestParam(required = false) UUID employeeId,
			@RequestParam(required = false) UUID trainingId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "completedAt,desc") String sort) {
		return page(employeeId, trainingId, page, size, sort);
	}

	@GetMapping("/training-completions/{completionId}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public CompletionResponse get(@PathVariable UUID completionId) { return service.get(completionId); }

	@PostMapping("/training-completions/{completionId}/recalculate-expiration") @PreAuthorize("hasRole('ADMIN')")
	public CompletionResponse recalculate(@PathVariable UUID completionId) { return service.recalculateExpiration(completionId); }

	@GetMapping({"/employees/{employeeId}/completions", "/employees/{employeeId}/training-history"})
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public PageResponse<CompletionResponse> employee(@PathVariable UUID employeeId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "completedAt,desc") String sort) {
		return page(employeeId, null, page, size, sort);
	}

	@GetMapping("/trainings/{trainingId}/completions") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<CompletionResponse> training(@PathVariable UUID trainingId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "completedAt,desc") String sort) {
		return page(null, trainingId, page, size, sort);
	}

	private PageResponse<CompletionResponse> page(UUID employeeId, UUID trainingId, int page, int size, String sort) {
		return PageResponse.from(service.list(employeeId, trainingId, pagination.create(page, size, sort, SORTABLE)));
	}
}
