package dev.igorbarbosa.worktrainingsystem.assignments.web;

import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentBatchResponse;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/training-assignment-batches")
public class AssignmentBatchController {
	private final AssignmentService service;
	public AssignmentBatchController(AssignmentService service) { this.service = service; }
	@GetMapping("/{batchId}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')")
	public AssignmentBatchResponse get(@PathVariable UUID batchId) { return service.getBatch(batchId); }
}
