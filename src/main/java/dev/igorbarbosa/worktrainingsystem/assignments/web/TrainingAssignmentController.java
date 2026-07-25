package dev.igorbarbosa.worktrainingsystem.assignments.web;

import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentBatchResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentReasonRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.api.BatchAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.CreateAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.UpdateAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentService;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/training-assignments")
public class TrainingAssignmentController {
	private static final String CAN_ASSIGN = "hasRole('ADMIN') or (hasAnyRole('MANAGER','SUPERVISOR') and hasAuthority('ASSIGN_TRAINING'))";
	private static final Set<String> SORTABLE = Set.of("assignedAt", "assignedDate", "dueDate", "status", "priority", "createdAt", "updatedAt");
	private final AssignmentService service;
	private final PaginationFactory pagination;
	private final AssignmentExecutionPort execution;
	public TrainingAssignmentController(AssignmentService service, PaginationFactory pagination, AssignmentExecutionPort execution) {
		this.service = service; this.pagination = pagination; this.execution = execution;
	}

	@PostMapping @PreAuthorize(CAN_ASSIGN)
	public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String key) {
		AssignmentResponse response = service.create(request, key);
		return ResponseEntity.created(URI.create("/api/v1/training-assignments/" + response.id())).body(response);
	}
	@PostMapping("/batch") @PreAuthorize(CAN_ASSIGN)
	public ResponseEntity<AssignmentBatchResponse> batch(@Valid @RequestBody BatchAssignmentRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String key) {
		return ResponseEntity.accepted().body(service.createBatch(request, key));
	}
	@GetMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public PageResponse<AssignmentResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "assignedAt,desc") String sort,
			@RequestParam(required = false) UUID employeeId, @RequestParam(required = false) UUID trainingId,
			@RequestParam(required = false) AssignmentStatus status, @RequestParam(required = false) AssignmentOrigin origin,
			@RequestParam(required = false) LocalDate dueFrom, @RequestParam(required = false) LocalDate dueTo) {
		return PageResponse.from(service.list(employeeId, trainingId, status, origin, dueFrom, dueTo,
				pagination.create(page, size, sort, SORTABLE)));
	}
	@GetMapping("/{assignmentId}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public AssignmentResponse get(@PathVariable UUID assignmentId) { return service.get(assignmentId); }
	@PostMapping("/{assignmentId}/start") @PreAuthorize("hasRole('EMPLOYEE')")
	public AssignmentExecutionPort.ExecutionAssignment start(@PathVariable UUID assignmentId) {
		return execution.start(assignmentId);
	}
	@PatchMapping("/{assignmentId}") @PreAuthorize(CAN_ASSIGN)
	public AssignmentResponse update(@PathVariable UUID assignmentId, @Valid @RequestBody UpdateAssignmentRequest request) {
		return service.update(assignmentId, request);
	}
	@PostMapping("/{assignmentId}/cancel") @PreAuthorize(CAN_ASSIGN)
	public AssignmentResponse cancel(@PathVariable UUID assignmentId, @Valid @RequestBody AssignmentReasonRequest request) {
		return service.cancel(assignmentId, request);
	}
	@PostMapping("/{assignmentId}/waive") @PreAuthorize("hasRole('ADMIN')")
	public AssignmentResponse waive(@PathVariable UUID assignmentId, @Valid @RequestBody AssignmentReasonRequest request) {
		return service.waive(assignmentId, request);
	}
	@PostMapping("/{assignmentId}/recycle") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AssignmentResponse> recycle(@PathVariable UUID assignmentId,
			@RequestHeader(name = "Idempotency-Key", required = false) String key) {
		AssignmentResponse response = service.recycle(assignmentId, key);
		return ResponseEntity.created(URI.create("/api/v1/training-assignments/" + response.id())).body(response);
	}
}
