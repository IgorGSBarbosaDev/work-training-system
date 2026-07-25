package dev.igorbarbosa.worktrainingsystem.activities.persistence;

import dev.igorbarbosa.worktrainingsystem.activities.domain.JobActivity;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobActivityRepository extends JpaRepository<JobActivity, UUID> {
	Optional<JobActivity> findByOrganizationIdAndJobIdAndActivityIdAndStatus(
			UUID organizationId, UUID jobId, UUID activityId, RegistrationStatus status);
	List<JobActivity> findAllByOrganizationIdAndJobIdAndStatusOrderByLinkedAt(
			UUID organizationId, UUID jobId, RegistrationStatus status);
	List<JobActivity> findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
			UUID organizationId, UUID activityId, RegistrationStatus status);
	@Query("""
			select distinct link.activityId from JobActivity link
			where link.organizationId = :organizationId and link.jobId in :jobIds
			and link.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			""")
	java.util.Set<UUID> findActiveActivityIds(UUID organizationId, java.util.Collection<UUID> jobIds);
}
