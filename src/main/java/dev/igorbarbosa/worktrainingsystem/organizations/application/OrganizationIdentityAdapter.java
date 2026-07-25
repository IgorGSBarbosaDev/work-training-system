package dev.igorbarbosa.worktrainingsystem.organizations.application;

import dev.igorbarbosa.worktrainingsystem.identity.application.OrganizationIdentityPort;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrganizationIdentityAdapter implements OrganizationIdentityPort {
	private final UnitRepository units;
	private final SectorRepository sectors;

	OrganizationIdentityAdapter(UnitRepository units, SectorRepository sectors) {
		this.units = units;
		this.sectors = sectors;
	}

	@Override
	public boolean unitExists(UUID organizationId, UUID unitId) {
		return units.existsByIdAndOrganizationId(unitId, organizationId);
	}

	@Override
	public boolean sectorExists(UUID organizationId, UUID sectorId) {
		return sectors.existsByIdAndOrganizationId(sectorId, organizationId);
	}
}
