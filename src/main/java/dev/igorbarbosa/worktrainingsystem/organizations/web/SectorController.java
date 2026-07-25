package dev.igorbarbosa.worktrainingsystem.organizations.web;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateSectorRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateSectorRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeService;
import dev.igorbarbosa.worktrainingsystem.organizations.application.SectorService;
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
@RequestMapping("/api/v1/sectors")
public class SectorController {

	private static final Set<String> SORTABLE_PROPERTIES =
			Set.of("name", "code", "status", "createdAt", "updatedAt");

	private final SectorService sectorService;
	private final PaginationFactory paginationFactory;
	private final EmployeeService employeeService;

	public SectorController(SectorService sectorService, PaginationFactory paginationFactory,
			EmployeeService employeeService) {
		this.sectorService = sectorService;
		this.paginationFactory = paginationFactory;
		this.employeeService = employeeService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<SectorResponse> create(@Valid @RequestBody CreateSectorRequest request) {
		SectorResponse response = sectorService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/sectors/" + response.id())).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<SectorResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) RegistrationStatus status,
			@RequestParam(required = false) UUID unitId) {
		return PageResponse.from(sectorService.list(
				search, status, unitId, paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}

	@GetMapping("/{sectorId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public SectorResponse get(@PathVariable UUID sectorId) { return sectorService.get(sectorId); }

	@PatchMapping("/{sectorId}")
	@PreAuthorize("hasRole('ADMIN')")
	public SectorResponse update(@PathVariable UUID sectorId, @Valid @RequestBody UpdateSectorRequest request) {
		return sectorService.update(sectorId, request);
	}

	@PatchMapping("/{sectorId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public SectorResponse changeStatus(@PathVariable UUID sectorId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return sectorService.changeStatus(sectorId, request);
	}

	@GetMapping("/{sectorId}/employees")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<EmployeeResponse> employees(
			@PathVariable UUID sectorId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort) {
		sectorService.get(sectorId);
		return PageResponse.from(employeeService.listBySector(sectorId,
				paginationFactory.create(page, size, sort,
						Set.of("name", "registration", "email", "status", "createdAt", "updatedAt"))));
	}
}
