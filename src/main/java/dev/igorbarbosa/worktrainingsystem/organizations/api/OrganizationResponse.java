package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Organization;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name, RegistrationStatus status,
		Instant createdAt, Instant updatedAt) {
	public static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(organization.getId(), organization.getName(), organization.getStatus(),
				organization.getCreatedAt(), organization.getUpdatedAt());
	}
}
