package dev.igorbarbosa.worktrainingsystem.jobs.web;

import dev.igorbarbosa.worktrainingsystem.jobs.api.CreateJobRequest;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobService;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

	private static final Set<String> SORTABLE_PROPERTIES =
			Set.of("name", "status", "createdAt", "updatedAt");

	private final JobService jobService;
	private final PaginationFactory paginationFactory;

	public JobController(JobService jobService, PaginationFactory paginationFactory) {
		this.jobService = jobService;
		this.paginationFactory = paginationFactory;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
		JobResponse response = jobService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/jobs/" + response.id())).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<JobResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) RegistrationStatus status) {
		return PageResponse.from(jobService.list(
				search, status, paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}
}
