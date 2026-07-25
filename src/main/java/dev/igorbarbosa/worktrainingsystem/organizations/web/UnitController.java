package dev.igorbarbosa.worktrainingsystem.organizations.web;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.application.SectorService;
import dev.igorbarbosa.worktrainingsystem.organizations.application.UnitService;
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
@RequestMapping("/api/v1/units")
public class UnitController {

	private static final Set<String> SORTABLE_PROPERTIES =
			Set.of("name", "code", "status", "createdAt", "updatedAt");

	private final UnitService unitService;
	private final PaginationFactory paginationFactory;
	private final SectorService sectorService;

	public UnitController(UnitService unitService, PaginationFactory paginationFactory, SectorService sectorService) {
		this.unitService = unitService;
		this.paginationFactory = paginationFactory;
		this.sectorService = sectorService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UnitResponse> create(@Valid @RequestBody CreateUnitRequest request) {
		UnitResponse response = unitService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/units/" + response.id())).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<UnitResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) RegistrationStatus status) {
		return PageResponse.from(unitService.list(
				search, status, paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}

	@GetMapping("/{unitId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public UnitResponse get(@PathVariable UUID unitId) { return unitService.get(unitId); }

	@PatchMapping("/{unitId}")
	@PreAuthorize("hasRole('ADMIN')")
	public UnitResponse update(@PathVariable UUID unitId, @Valid @RequestBody UpdateUnitRequest request) {
		return unitService.update(unitId, request);
	}

	@PatchMapping("/{unitId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public UnitResponse changeStatus(@PathVariable UUID unitId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return unitService.changeStatus(unitId, request);
	}

	@GetMapping("/{unitId}/sectors")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<SectorResponse> sectors(
			@PathVariable UUID unitId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort) {
		unitService.get(unitId);
		return PageResponse.from(sectorService.listByUnit(unitId,
				paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}
}
