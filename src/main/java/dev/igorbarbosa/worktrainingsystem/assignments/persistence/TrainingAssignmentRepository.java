package dev.igorbarbosa.worktrainingsystem.assignments.persistence;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;

public interface TrainingAssignmentRepository extends JpaRepository<TrainingAssignment, UUID>,
		JpaSpecificationExecutor<TrainingAssignment> {
	long countByOrganizationId(UUID organizationId);
	long countByOrganizationIdAndStatus(UUID organizationId, AssignmentStatus status);
	long countByOrganizationIdAndEmployeeIdAndStatus(UUID organizationId, UUID employeeId, AssignmentStatus status);
	long countByOrganizationIdAndEmployeeIdIn(UUID organizationId, Collection<UUID> employeeIds);
	long countByOrganizationIdAndEmployeeIdInAndStatus(UUID organizationId, Collection<UUID> employeeIds,
			AssignmentStatus status);
	Optional<TrainingAssignment> findByIdAndOrganizationId(UUID id, UUID organizationId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select assignment from TrainingAssignment assignment where assignment.id = :id and assignment.organizationId = :organizationId")
	Optional<TrainingAssignment> lockByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<TrainingAssignment> findByOrganizationIdAndResponsibleUserIdAndIdempotencyKey(
			UUID organizationId, UUID responsibleUserId, String idempotencyKey);
	Optional<TrainingAssignment> findFirstByOrganizationIdAndEmployeeIdAndTrainingIdAndTrainingVersionIdAndStatusIn(
			UUID organizationId, UUID employeeId, UUID trainingId, UUID trainingVersionId,
			Collection<AssignmentStatus> statuses);
	Optional<TrainingAssignment> findFirstByOrganizationIdAndEmployeeIdAndTrainingVersionIdAndStatusIn(
			UUID organizationId, UUID employeeId, UUID trainingVersionId, Collection<AssignmentStatus> statuses);

	@Modifying
	@Query(value = """
			insert into training_assignments (
			 id, organization_id, employee_id, training_id, training_version_id, origin,
			 assigned_at, assigned_date, due_date, status, priority, responsible_user_id,
			 recertification, recertification_of_assignment_id, idempotency_key, request_hash,
			 batch_id, created_at, updated_at, version)
			values (:id, :organizationId, :employeeId, :trainingId, :trainingVersionId, :origin,
			 :assignedAt, :assignedDate, :dueDate, 'NOT_STARTED', :priority, :actor,
			 :recertification, :recertificationOf, :idempotencyKey, :requestHash,
			 :batchId, :assignedAt, :assignedAt, 0)
			on conflict do nothing
			""", nativeQuery = true)
	int insertIfAbsent(UUID id, UUID organizationId, UUID employeeId, UUID trainingId,
			UUID trainingVersionId, String origin, Instant assignedAt, LocalDate assignedDate,
			LocalDate dueDate, String priority, UUID actor, boolean recertification,
			UUID recertificationOf, String idempotencyKey, String requestHash, UUID batchId);

	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into training_assignments (
			 id, organization_id, employee_id, training_id, training_version_id, origin,
			 assigned_at, assigned_date, due_date, status, priority, responsible_user_id,
			 recertification, recertification_of_completion_id, created_at, updated_at, version)
			values (:id, :organizationId, :employeeId, :trainingId, :trainingVersionId, 'RECERTIFICATION',
			 :assignedAt, :assignedDate, :dueDate, 'NOT_STARTED', :priority, :actor,
			 true, :completionId, :assignedAt, :assignedAt, 0)
			on conflict do nothing
			""", nativeQuery = true)
	int insertRecertificationIfAbsent(UUID id, UUID organizationId, UUID employeeId, UUID trainingId,
			UUID trainingVersionId, Instant assignedAt, LocalDate assignedDate, LocalDate dueDate,
			String priority, UUID actor, UUID completionId);
}
