package dev.igorbarbosa.worktrainingsystem.organizations.web;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateSectorRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
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

	public SectorController(SectorService sectorService, PaginationFactory paginationFactory) {
		this.sectorService = sectorService;
		this.paginationFactory = paginationFactory;
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
}
