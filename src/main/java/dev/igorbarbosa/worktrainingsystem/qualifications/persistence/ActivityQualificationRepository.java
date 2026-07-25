package dev.igorbarbosa.worktrainingsystem.qualifications.persistence;

import dev.igorbarbosa.worktrainingsystem.qualifications.domain.ActivityQualification;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ActivityQualificationRepository extends JpaRepository<ActivityQualification, UUID>,
		JpaSpecificationExecutor<ActivityQualification> {
	Optional<ActivityQualification> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<ActivityQualification> findByOrganizationIdAndEmployeeIdAndActivityId(UUID organizationId,
			UUID employeeId, UUID activityId);
	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into activity_qualifications (id, organization_id, employee_id, activity_id, status,
			 calculated_at, next_expiration_date, blocking_reasons, created_at, updated_at, version)
			values (:id, :organizationId, :employeeId, :activityId, :status, :calculatedAt,
			 :nextExpirationDate, cast(:blockingReasons as jsonb), :calculatedAt, :calculatedAt, 0)
			on conflict (organization_id, employee_id, activity_id) do update set
			 status = excluded.status, calculated_at = excluded.calculated_at,
			 next_expiration_date = excluded.next_expiration_date,
			 blocking_reasons = excluded.blocking_reasons, updated_at = excluded.updated_at,
			 version = activity_qualifications.version + 1
			""", nativeQuery = true)
	int upsert(UUID id, UUID organizationId, UUID employeeId, UUID activityId, String status,
			Instant calculatedAt, LocalDate nextExpirationDate, String blockingReasons);
}
