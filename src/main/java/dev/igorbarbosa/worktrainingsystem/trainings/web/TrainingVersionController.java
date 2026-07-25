package dev.igorbarbosa.worktrainingsystem.trainings.web;

import dev.igorbarbosa.worktrainingsystem.trainings.api.ModuleRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ModuleResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ContentSummaryResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.OrderRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalogService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingVersionController {

	private final TrainingCatalogService service;

	public TrainingVersionController(TrainingCatalogService service) {
		this.service = service;
	}

	@GetMapping("/api/v1/trainings/{trainingId}/versions")
	@PreAuthorize("hasRole('ADMIN')")
	public List<TrainingVersionResponse> list(@PathVariable UUID trainingId) {
		return service.listVersions(trainingId);
	}

	@PostMapping("/api/v1/trainings/{trainingId}/versions")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TrainingVersionResponse> create(@PathVariable UUID trainingId,
			@Valid @RequestBody TrainingVersionRequest request) {
		TrainingVersionResponse response = service.createVersion(trainingId, request);
		return ResponseEntity.created(URI.create("/api/v1/training-versions/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/training-versions/{versionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingVersionResponse get(@PathVariable UUID versionId) {
		return service.getVersion(versionId);
	}

	@PatchMapping("/api/v1/training-versions/{versionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingVersionResponse update(@PathVariable UUID versionId,
			@Valid @RequestBody TrainingVersionRequest request) {
		return service.updateVersion(versionId, request);
	}

	@PostMapping("/api/v1/training-versions/{versionId}/publish")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingVersionResponse publish(@PathVariable UUID versionId) {
		return service.publishVersion(versionId);
	}

	@PostMapping("/api/v1/training-versions/{versionId}/archive")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingVersionResponse archive(@PathVariable UUID versionId) {
		return service.archiveVersion(versionId);
	}

	@PostMapping("/api/v1/training-versions/{versionId}/duplicate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TrainingVersionResponse> duplicate(@PathVariable UUID versionId) {
		TrainingVersionResponse response = service.duplicateVersion(versionId);
		return ResponseEntity.created(URI.create("/api/v1/training-versions/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/training-versions/{versionId}/content-summary")
	@PreAuthorize("hasRole('ADMIN')")
	public ContentSummaryResponse contentSummary(@PathVariable UUID versionId) { return service.contentSummary(versionId); }

	@PostMapping("/api/v1/training-versions/{versionId}/modules")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ModuleResponse> createModule(@PathVariable UUID versionId,
			@Valid @RequestBody ModuleRequest request) {
		ModuleResponse response = service.createModule(versionId, request);
		return ResponseEntity.created(URI.create("/api/v1/modules/" + response.id())).body(response);
	}

	@GetMapping("/api/v1/training-versions/{versionId}/modules")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ModuleResponse> listModules(@PathVariable UUID versionId) {
		return service.listModules(versionId);
	}

	@PatchMapping("/api/v1/training-versions/{versionId}/modules/order")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ModuleResponse> reorderModules(@PathVariable UUID versionId, @Valid @RequestBody OrderRequest request) {
		return service.reorderModules(versionId, request);
	}

	@PatchMapping("/api/v1/modules/{moduleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ModuleResponse updateModule(@PathVariable UUID moduleId, @Valid @RequestBody ModuleRequest request) {
		return service.updateModule(moduleId, request);
	}

	@GetMapping("/api/v1/modules/{moduleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ModuleResponse getModule(@PathVariable UUID moduleId) {
		return service.getModule(moduleId);
	}

	@PatchMapping("/api/v1/modules/{moduleId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ModuleResponse moduleStatus(@PathVariable UUID moduleId,
			@Valid @RequestBody ChangeRegistrationStatusRequest request) {
		return service.changeModuleStatus(moduleId, request.status());
	}
}
