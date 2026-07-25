package dev.igorbarbosa.worktrainingsystem.assignments.web;

import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentService;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/training-assignments")
@PreAuthorize("hasRole('EMPLOYEE')")
public class MyTrainingAssignmentController {
	private static final Set<String> SORTABLE = Set.of("assignedAt", "dueDate", "status", "priority");
	private final AssignmentService service;
	private final PaginationFactory pagination;
	public MyTrainingAssignmentController(AssignmentService service, PaginationFactory pagination) {
		this.service = service; this.pagination = pagination;
	}
	@GetMapping
	public PageResponse<AssignmentResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "assignedAt,desc") String sort,
			@RequestParam(required = false) AssignmentStatus status) {
		return PageResponse.from(service.mine(status, pagination.create(page, size, sort, SORTABLE)));
	}
}
