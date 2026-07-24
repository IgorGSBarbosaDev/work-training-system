package dev.igorbarbosa.worktrainingsystem.employees.web;

import dev.igorbarbosa.worktrainingsystem.employees.api.CreateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeJobRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeJobResponse;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeStatusRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.employees.api.UpdateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeService;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

	private static final Set<String> SORTABLE_PROPERTIES =
			Set.of("name", "registration", "email", "status", "createdAt", "updatedAt");

	private final EmployeeService employeeService;
	private final PaginationFactory paginationFactory;

	public EmployeeController(EmployeeService employeeService, PaginationFactory paginationFactory) {
		this.employeeService = employeeService;
		this.paginationFactory = paginationFactory;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
		EmployeeResponse response = employeeService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/employees/" + response.id())).body(response);
	}

	@GetMapping("/{employeeId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public EmployeeResponse getById(@PathVariable UUID employeeId) {
		return employeeService.getById(employeeId);
	}

	@GetMapping("/by-registration/{registration}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public EmployeeResponse getByRegistration(
			@PathVariable @Size(max = 50, message = "A matrícula deve possuir no máximo 50 caracteres.")
			String registration) {
		return employeeService.getByRegistration(registration);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<EmployeeResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String registration,
			@RequestParam(required = false) String email,
			@RequestParam(required = false) UUID unitId,
			@RequestParam(required = false) UUID sectorId,
			@RequestParam(required = false) UUID jobId,
			@RequestParam(required = false) RegistrationStatus status) {
		return PageResponse.from(employeeService.list(
				search,
				registration,
				email,
				unitId,
				sectorId,
				jobId,
				status,
				paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}

	@PatchMapping("/{employeeId}")
	@PreAuthorize("hasRole('ADMIN')")
	public EmployeeResponse update(
			@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeRequest request) {
		return employeeService.update(employeeId, request);
	}

	@PatchMapping("/{employeeId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public EmployeeResponse changeStatus(
			@PathVariable UUID employeeId, @Valid @RequestBody ChangeEmployeeStatusRequest request) {
		return employeeService.changeStatus(employeeId, request);
	}

	@PatchMapping("/{employeeId}/job")
	@PreAuthorize("hasRole('ADMIN')")
	public ChangeEmployeeJobResponse changeJob(
			@PathVariable UUID employeeId, @Valid @RequestBody ChangeEmployeeJobRequest request) {
		return employeeService.changeJob(employeeId, request);
	}
}
