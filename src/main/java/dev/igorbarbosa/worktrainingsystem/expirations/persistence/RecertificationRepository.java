package dev.igorbarbosa.worktrainingsystem.expirations.persistence;

import dev.igorbarbosa.worktrainingsystem.expirations.domain.Recertification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecertificationRepository extends JpaRepository<Recertification, UUID>, JpaSpecificationExecutor<Recertification> {
	Optional<Recertification> findByOrganizationIdAndCompletionId(UUID organizationId, UUID completionId);
}
