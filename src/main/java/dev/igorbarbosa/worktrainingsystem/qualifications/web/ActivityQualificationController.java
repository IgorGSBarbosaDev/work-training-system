package dev.igorbarbosa.worktrainingsystem.qualifications.web;

import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationRecalculationResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationService;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities/{activityId}")
public class ActivityQualificationController {
	private static final Set<String> SORTABLE = Set.of("status", "calculatedAt", "nextExpirationDate");
	private final QualificationService service;
	private final PaginationFactory pagination;
	public ActivityQualificationController(QualificationService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}
	@GetMapping("/qualified-employees") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<QualificationResponse> list(@PathVariable UUID activityId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "status,asc") String sort,
			@RequestParam(required = false) QualificationStatus status) {
		return PageResponse.from(service.list(null, activityId, status, pagination.create(page, size, sort, SORTABLE)));
	}
	@PostMapping("/qualifications/recalculate") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<QualificationRecalculationResponse> recalculate(@PathVariable UUID activityId) {
		return ResponseEntity.accepted().body(new QualificationRecalculationResponse(service.recalculateActivity(activityId)));
	}
}
