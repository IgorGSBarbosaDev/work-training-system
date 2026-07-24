package dev.igorbarbosa.worktrainingsystem.trainings.web;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ChangeTrainingStatusRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.CreateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.UpdateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trainings")
public class TrainingController {

	private static final Set<String> SORTABLE_PROPERTIES = Set.of("name", "code", "status", "createdAt", "updatedAt");

	private final TrainingCatalogService service;
	private final PaginationFactory paginationFactory;

	public TrainingController(TrainingCatalogService service, PaginationFactory paginationFactory) {
		this.service = service;
		this.paginationFactory = paginationFactory;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TrainingResponse> create(@Valid @RequestBody CreateTrainingRequest request) {
		TrainingResponse response = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/trainings/" + response.id())).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public PageResponse<TrainingResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) RegistrationStatus status) {
		return PageResponse.from(service.list(search, status, paginationFactory.create(page, size, sort, SORTABLE_PROPERTIES)));
	}

	@GetMapping("/{trainingId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR')")
	public TrainingResponse get(@PathVariable UUID trainingId) {
		return service.getTraining(trainingId);
	}

	@PatchMapping("/{trainingId}")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingResponse update(@PathVariable UUID trainingId, @Valid @RequestBody UpdateTrainingRequest request) {
		return service.updateTraining(trainingId, request);
	}

	@PatchMapping("/{trainingId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public TrainingResponse changeStatus(@PathVariable UUID trainingId,
			@Valid @RequestBody ChangeTrainingStatusRequest request) {
		return service.changeTrainingStatus(trainingId, request);
	}
}
