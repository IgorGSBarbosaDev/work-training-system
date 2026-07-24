package dev.igorbarbosa.worktrainingsystem.jobs.persistence;

import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

	boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

	Optional<Job> findByIdAndOrganizationId(UUID id, UUID organizationId);

	Set<Job> findAllByIdInAndOrganizationId(Set<UUID> ids, UUID organizationId);
}
