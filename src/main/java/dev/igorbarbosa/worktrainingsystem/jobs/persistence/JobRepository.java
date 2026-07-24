package dev.igorbarbosa.worktrainingsystem.jobs.persistence;

import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

	boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
