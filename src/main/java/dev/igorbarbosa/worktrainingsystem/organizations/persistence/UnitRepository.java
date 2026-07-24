package dev.igorbarbosa.worktrainingsystem.organizations.persistence;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnitRepository extends JpaRepository<Unit, UUID>, JpaSpecificationExecutor<Unit> {

	boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

	boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

	Optional<Unit> findByIdAndOrganizationId(UUID id, UUID organizationId);

	Set<Unit> findAllByIdInAndOrganizationId(Set<UUID> ids, UUID organizationId);
}
