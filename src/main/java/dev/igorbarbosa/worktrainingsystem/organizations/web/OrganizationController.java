package dev.igorbarbosa.worktrainingsystem.organizations.web;

import dev.igorbarbosa.worktrainingsystem.organizations.api.OrganizationResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.OrganizationSettingsResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateOrganizationRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateOrganizationSettingsRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.application.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {
	private final OrganizationService service;

	public OrganizationController(OrganizationService service) { this.service = service; }

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public OrganizationResponse get() { return service.get(); }

	@PatchMapping
	@PreAuthorize("hasRole('ADMIN')")
	public OrganizationResponse update(@Valid @RequestBody UpdateOrganizationRequest request) {
		return service.update(request);
	}

	@GetMapping("/settings")
	@PreAuthorize("hasRole('ADMIN')")
	public OrganizationSettingsResponse getSettings() { return service.getSettings(); }

	@PatchMapping("/settings")
	@PreAuthorize("hasRole('ADMIN')")
	public OrganizationSettingsResponse updateSettings(
			@Valid @RequestBody UpdateOrganizationSettingsRequest request) {
		return service.updateSettings(request);
	}
}
