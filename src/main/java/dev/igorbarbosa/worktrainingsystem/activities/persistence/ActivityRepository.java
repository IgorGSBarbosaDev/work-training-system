package dev.igorbarbosa.worktrainingsystem.activities.persistence;

import dev.igorbarbosa.worktrainingsystem.activities.domain.Activity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {
	boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
	Optional<Activity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
