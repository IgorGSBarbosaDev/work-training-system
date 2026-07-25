package dev.igorbarbosa.worktrainingsystem.activities.persistence;

import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeActivityRepository extends JpaRepository<EmployeeActivity, UUID> {
	Optional<EmployeeActivity> findByOrganizationIdAndEmployeeIdAndActivityIdAndOriginAndStatus(
			UUID organizationId, UUID employeeId, UUID activityId, EmployeeActivityOrigin origin,
			RegistrationStatus status);
	Optional<EmployeeActivity> findByOrganizationIdAndEmployeeIdAndActivityIdAndSourceJobActivityIdAndStatus(
			UUID organizationId, UUID employeeId, UUID activityId, UUID sourceJobActivityId, RegistrationStatus status);
	List<EmployeeActivity> findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
			UUID organizationId, UUID employeeId, RegistrationStatus status);
	List<EmployeeActivity> findAllByOrganizationIdAndSourceJobActivityIdAndStatus(
			UUID organizationId, UUID sourceJobActivityId, RegistrationStatus status);
	@Query("""
			select distinct link.employeeId from EmployeeActivity link
			where link.organizationId = :organizationId and link.activityId = :activityId
			and link.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			""")
	Set<UUID> findActiveEmployeeIds(UUID organizationId, UUID activityId);
	@Query("""
			select distinct link.activityId from EmployeeActivity link
			where link.organizationId = :organizationId and link.employeeId in :employeeIds
			and link.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			""")
	Set<UUID> findActiveActivityIds(UUID organizationId, Collection<UUID> employeeIds);
	@Query("""
			select distinct link.employeeId from EmployeeActivity link
			where link.organizationId = :organizationId and link.activityId = :activityId
			and link.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			""")
	Page<UUID> findActiveEmployeeIds(UUID organizationId, UUID activityId, Pageable pageable);
	@Query("""
			select distinct link.employeeId from EmployeeActivity link
			where link.organizationId = :organizationId and link.activityId = :activityId
			and link.employeeId in :employeeIds
			and link.status = dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus.ACTIVE
			""")
	Page<UUID> findActiveEmployeeIdsInScope(UUID organizationId, UUID activityId,
			Collection<UUID> employeeIds, Pageable pageable);
}
