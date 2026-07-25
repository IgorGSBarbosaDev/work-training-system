package dev.igorbarbosa.worktrainingsystem.expirations.web;

import dev.igorbarbosa.worktrainingsystem.expirations.api.ExpirationRecalculationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.api.ExpirationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.api.RecertificationRequest;
import dev.igorbarbosa.worktrainingsystem.expirations.api.RecertificationResponse;
import dev.igorbarbosa.worktrainingsystem.expirations.application.ExpirationService;
import dev.igorbarbosa.worktrainingsystem.expirations.domain.ExpirationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1")
public class ExpirationController {
	private static final Set<String> EXPIRATION_SORT = Set.of("expirationDate", "completionDate", "employeeId", "trainingId");
	private static final Set<String> RECERTIFICATION_SORT = Set.of("createdAt", "completionId", "assignmentId");
	private final ExpirationService service; private final PaginationFactory pagination;
	public ExpirationController(ExpirationService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}
	@GetMapping("/expirations") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<ExpirationResponse> list(@RequestParam(required = false) UUID employeeId,
			@RequestParam(required = false) UUID trainingId, @RequestParam(required = false) UUID unitId,
			@RequestParam(required = false) UUID sectorId, @RequestParam(required = false) UUID jobId,
			@RequestParam(required = false) ExpirationStatus status,
			@RequestParam(required = false) LocalDate expiresFrom, @RequestParam(required = false) LocalDate expiresTo,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "expirationDate,asc") String sort) {
		return PageResponse.from(service.list(employeeId, trainingId, unitId, sectorId, jobId, status,
				expiresFrom, expiresTo, pagination.create(page, size, sort, EXPIRATION_SORT)));
	}
	@PostMapping("/expirations/recalculate") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ExpirationRecalculationResponse> recalculate() {
		return ResponseEntity.accepted().body(service.recalculate());
	}
	@PostMapping("/recertifications") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RecertificationResponse> create(@Valid @RequestBody RecertificationRequest request) {
		var response = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/recertifications/" + response.id())).body(response);
	}
	@GetMapping("/recertifications") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<RecertificationResponse> recertifications(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort) {
		return PageResponse.from(service.list(pagination.create(page, size, sort, RECERTIFICATION_SORT)));
	}
}
