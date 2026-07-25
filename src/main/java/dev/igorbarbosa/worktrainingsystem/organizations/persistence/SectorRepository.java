package dev.igorbarbosa.worktrainingsystem.organizations.persistence;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SectorRepository extends JpaRepository<Sector, UUID>, JpaSpecificationExecutor<Sector> {

	boolean existsByUnitIdAndNameIgnoreCase(UUID unitId, String name);

	boolean existsByUnitIdAndCodeIgnoreCase(UUID unitId, String code);

	Optional<Sector> findByIdAndOrganizationId(UUID id, UUID organizationId);
	boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);

	Set<Sector> findAllByIdInAndOrganizationId(Set<UUID> ids, UUID organizationId);
}
