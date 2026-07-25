package dev.igorbarbosa.worktrainingsystem.activities.web;

import dev.igorbarbosa.worktrainingsystem.activities.api.JobActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.JobActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityService;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/activities")
public class JobActivityController {
	private final ActivityService service;
	private final JobService jobs;
	public JobActivityController(ActivityService service, JobService jobs) { this.service = service; this.jobs = jobs; }
	@GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public List<JobActivityResponse> list(@PathVariable UUID jobId) { jobs.get(jobId); return service.listJobActivities(jobId); }
	@PostMapping @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<JobActivityResponse> add(@PathVariable UUID jobId, @Valid @RequestBody JobActivityRequest request) {
		JobActivityResponse response = service.addJobActivity(jobId, request);
		return ResponseEntity.created(URI.create("/api/v1/jobs/" + jobId + "/activities/" + response.activity().id())).body(response);
	}
	@DeleteMapping("/{activityId}") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> remove(@PathVariable UUID jobId, @PathVariable UUID activityId) {
		service.removeJobActivity(jobId, activityId); return ResponseEntity.noContent().build();
	}
}
