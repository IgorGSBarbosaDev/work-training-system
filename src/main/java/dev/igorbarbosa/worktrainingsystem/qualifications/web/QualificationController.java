package dev.igorbarbosa.worktrainingsystem.qualifications.web;

import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationService;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qualifications")
public class QualificationController {
	private static final Set<String> SORTABLE = Set.of("calculatedAt", "nextExpirationDate", "status", "createdAt", "updatedAt");
	private final QualificationService service;
	private final PaginationFactory pagination;
	public QualificationController(QualificationService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}
	@GetMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<QualificationResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "calculatedAt,desc") String sort,
			@RequestParam(required = false) UUID employeeId, @RequestParam(required = false) UUID activityId,
			@RequestParam(required = false) QualificationStatus status) {
		return PageResponse.from(service.list(employeeId, activityId, status,
				pagination.create(page, size, sort, SORTABLE)));
	}
	@GetMapping("/{qualificationId}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public QualificationResponse get(@PathVariable UUID qualificationId) { return service.get(qualificationId); }
}
