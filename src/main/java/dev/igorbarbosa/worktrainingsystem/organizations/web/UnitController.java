package dev.igorbarbosa.worktrainingsystem.organizations.web;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.application.UnitService;
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
@RequestMapping("/api/v1/units")
public class UnitController {

	private static final Set<String> SORTABLE_PROPERTIES =
			Set.of("name", "code", "status", "createdAt", "updatedAt");

	private final UnitService unitService;
	private final PaginationFactory paginationFactory;

	public UnitController(UnitService unitService, PaginationFactory paginationFactory) {
		this.unitService = unitService;
		this.paginationFactory = paginationFactory;
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
}
