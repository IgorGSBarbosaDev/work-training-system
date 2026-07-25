package dev.igorbarbosa.worktrainingsystem.jobs.web;

import dev.igorbarbosa.worktrainingsystem.jobs.api.CreateJobRequest;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.api.UpdateJobRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeService;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobService;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final EmployeeService employeeService;

	public JobController(JobService jobService, PaginationFactory paginationFactory,
			EmployeeService employeeService) {
		this.jobService = jobService;
		this.paginationFactory = paginationFactory;
		this.employeeService = employeeService;
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

	@GetMapping("/{jobId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public JobResponse get(@PathVariable UUID jobId) { return jobService.get(jobId); }

	@PatchMapping("/{jobId}")
	@PreAuthorize("hasRole('ADMIN')")
	public JobResponse update(@PathVariable UUID jobId, @Valid @RequestBody UpdateJobRequest request) {
		return jobService.update(jobId, request);
	}

	@PatchMapping("/{jobId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public JobResponse changeStatus(@PathVariable UUID jobId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return jobService.changeStatus(jobId, request);
	}

	@GetMapping("/{jobId}/employees")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<EmployeeResponse> employees(
			@PathVariable UUID jobId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort) {
		jobService.get(jobId);
		return PageResponse.from(employeeService.listByJob(jobId,
				paginationFactory.create(page, size, sort,
						Set.of("name", "registration", "email", "status", "createdAt", "updatedAt"))));
	}
}
