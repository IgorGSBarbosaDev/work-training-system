package dev.igorbarbosa.worktrainingsystem.organizations.persistence;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SectorRepository extends JpaRepository<Sector, UUID>, JpaSpecificationExecutor<Sector> {

	boolean existsByUnitIdAndNameIgnoreCase(UUID unitId, String name);

	boolean existsByUnitIdAndCodeIgnoreCase(UUID unitId, String code);
}
