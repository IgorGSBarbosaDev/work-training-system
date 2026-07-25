package dev.igorbarbosa.worktrainingsystem.progress.web;

import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentService;
import dev.igorbarbosa.worktrainingsystem.progress.api.MyAssignmentDetailResponse;
import dev.igorbarbosa.worktrainingsystem.progress.application.VideoProgressService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/training-assignments")
@PreAuthorize("hasRole('EMPLOYEE')")
public class MyAssignmentDetailController {
	private final AssignmentService assignments;
	private final VideoProgressService progress;
	public MyAssignmentDetailController(AssignmentService assignments, VideoProgressService progress) {
		this.assignments = assignments; this.progress = progress;
	}
	@GetMapping("/{assignmentId}")
	public MyAssignmentDetailResponse get(@PathVariable UUID assignmentId) {
		return new MyAssignmentDetailResponse(assignments.mine(assignmentId), progress.ownLearningPath(assignmentId),
				progress.resumePoint(assignmentId));
	}
}
