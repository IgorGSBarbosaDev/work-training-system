package dev.igorbarbosa.worktrainingsystem.activities.web;

import dev.igorbarbosa.worktrainingsystem.activities.api.ActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.ActivityResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.RelatedJobResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.RequirementRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.RequirementResponse;
import dev.igorbarbosa.worktrainingsystem.activities.api.UpdateActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.UpdateRequirementRequest;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityService;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
	private static final Set<String> SORTABLE = Set.of("name", "status", "createdAt", "updatedAt");
	private final ActivityService service;
	private final PaginationFactory pagination;
	public ActivityController(ActivityService service, PaginationFactory pagination) { this.service = service; this.pagination = pagination; }

	@PostMapping @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
		ActivityResponse response = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/activities/" + response.id())).body(response);
	}
	@GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<ActivityResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search, @RequestParam(required = false) RegistrationStatus status,
			@RequestParam(required = false) LocalDate createdFrom, @RequestParam(required = false) LocalDate createdTo) {
		return PageResponse.from(service.list(search, status, createdFrom, createdTo,
				pagination.create(page, size, sort, SORTABLE)));
	}
	@GetMapping("/{activityId}") @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public ActivityResponse get(@PathVariable UUID activityId) { return service.get(activityId); }
	@PatchMapping("/{activityId}") @PreAuthorize("hasRole('ADMIN')")
	public ActivityResponse update(@PathVariable UUID activityId, @Valid @RequestBody UpdateActivityRequest request) {
		return service.update(activityId, request);
	}
	@PatchMapping("/{activityId}/status") @PreAuthorize("hasRole('ADMIN')")
	public ActivityResponse status(@PathVariable UUID activityId, @Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeStatus(activityId, request.status());
	}
	@GetMapping("/{activityId}/jobs") @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public List<RelatedJobResponse> jobs(@PathVariable UUID activityId) { return service.listActivityJobs(activityId); }
	@GetMapping("/{activityId}/requirements") @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public List<RequirementResponse> requirements(@PathVariable UUID activityId) { return service.listRequirements(activityId); }
	@PostMapping("/{activityId}/requirements") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RequirementResponse> addRequirement(@PathVariable UUID activityId,
			@Valid @RequestBody RequirementRequest request) {
		RequirementResponse response = service.addRequirement(activityId, request);
		return ResponseEntity.created(URI.create("/api/v1/activities/" + activityId + "/requirements/" + response.id())).body(response);
	}
	@PatchMapping("/{activityId}/requirements/{requirementId}") @PreAuthorize("hasRole('ADMIN')")
	public RequirementResponse updateRequirement(@PathVariable UUID activityId, @PathVariable UUID requirementId,
			@Valid @RequestBody UpdateRequirementRequest request) {
		return service.updateRequirement(activityId, requirementId, request);
	}
	@DeleteMapping("/{activityId}/requirements/{requirementId}") @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> removeRequirement(@PathVariable UUID activityId, @PathVariable UUID requirementId) {
		service.removeRequirement(activityId, requirementId); return ResponseEntity.noContent().build();
	}
}
