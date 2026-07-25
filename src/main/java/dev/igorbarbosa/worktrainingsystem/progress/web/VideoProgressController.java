package dev.igorbarbosa.worktrainingsystem.progress.web;

import dev.igorbarbosa.worktrainingsystem.progress.api.LearningPathResponse;
import dev.igorbarbosa.worktrainingsystem.progress.api.ResumePointResponse;
import dev.igorbarbosa.worktrainingsystem.progress.api.VideoProgressRequest;
import dev.igorbarbosa.worktrainingsystem.progress.api.VideoProgressResponse;
import dev.igorbarbosa.worktrainingsystem.progress.application.VideoProgressService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/training-assignments/{assignmentId}")
public class VideoProgressController {
	private final VideoProgressService service;
	public VideoProgressController(VideoProgressService service) { this.service = service; }
	@GetMapping("/learning-path") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public LearningPathResponse learningPath(@PathVariable UUID assignmentId) { return service.learningPath(assignmentId); }
	@GetMapping("/resume-point") @PreAuthorize("hasRole('EMPLOYEE')")
	public ResumePointResponse resume(@PathVariable UUID assignmentId) { return service.resumePoint(assignmentId); }
	@GetMapping("/videos/{videoId}/progress") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR','EMPLOYEE')")
	public VideoProgressResponse get(@PathVariable UUID assignmentId, @PathVariable UUID videoId) {
		return service.get(assignmentId, videoId);
	}
	@PutMapping("/videos/{videoId}/progress") @PreAuthorize("hasRole('EMPLOYEE')")
	public VideoProgressResponse update(@PathVariable UUID assignmentId, @PathVariable UUID videoId,
			@Valid @RequestBody VideoProgressRequest request) { return service.update(assignmentId, videoId, request); }
}
