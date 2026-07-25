package dev.igorbarbosa.worktrainingsystem.activities.web;

import dev.igorbarbosa.worktrainingsystem.activities.api.EmployeeActivityDetailResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.EmployeeActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.ManualEmployeeActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityService;
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
@RequestMapping("/api/v1/employees/{employeeId}/activities")
public class EmployeeActivityController {
	private final ActivityService service;
	public EmployeeActivityController(ActivityService service) { this.service = service; }
	@GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE')")
	public List<EmployeeActivityResponse> list(@PathVariable UUID employeeId) { return service.listEmployeeActivities(employeeId); }
	@GetMapping("/{activityId}") @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE')")
	public EmployeeActivityDetailResponse get(@PathVariable UUID employeeId, @PathVariable UUID activityId) {
		return service.getEmployeeActivity(employeeId, activityId);
	}
	@PostMapping @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EmployeeActivityResponse> add(@PathVariable UUID employeeId,
			@Valid @RequestBody ManualEmployeeActivityRequest request) {
		EmployeeActivityResponse response = service.addManualEmployeeActivity(employeeId, request);
		return ResponseEntity.created(URI.create("/api/v1/employees/" + employeeId + "/activities/" + response.activity().id())).body(response);
	}
	@DeleteMapping("/{activityId}") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> remove(@PathVariable UUID employeeId, @PathVariable UUID activityId) {
		service.removeManualEmployeeActivity(employeeId, activityId); return ResponseEntity.noContent().build();
	}
}
