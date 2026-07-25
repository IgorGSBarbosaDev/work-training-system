package dev.igorbarbosa.worktrainingsystem.identity.application;

import java.util.UUID;

public interface OrganizationIdentityPort {
	boolean unitExists(UUID organizationId, UUID unitId);
	boolean sectorExists(UUID organizationId, UUID sectorId);
}
