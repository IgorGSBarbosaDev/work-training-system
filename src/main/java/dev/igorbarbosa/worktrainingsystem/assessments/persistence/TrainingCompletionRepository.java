package dev.igorbarbosa.worktrainingsystem.assessments.persistence;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TrainingCompletionRepository extends JpaRepository<TrainingCompletion, UUID>,
		JpaSpecificationExecutor<TrainingCompletion> {
	long countByOrganizationId(UUID organizationId);
	@Query(value = "select count(*) from training_completions where organization_id = :organizationId and expiration_date < :date", nativeQuery = true)
	long countExpired(UUID organizationId, LocalDate date);
	@Query(value = "select count(*) from training_completions where organization_id = :organizationId and expiration_date >= :date and expiration_date <= :until", nativeQuery = true)
	long countExpiring(UUID organizationId, LocalDate date, LocalDate until);
	Optional<TrainingCompletion> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<TrainingCompletion> findByOrganizationIdAndSourceAssignmentId(UUID organizationId, UUID assignmentId);
	Optional<TrainingCompletion> findFirstByOrganizationIdAndEmployeeIdAndTrainingIdOrderByCompletedAtDescIdDesc(
			UUID organizationId, UUID employeeId, UUID trainingId);
	List<TrainingCompletion> findAllByOrganizationIdAndEmployeeIdAndTrainingIdOrderByCompletedAtDescIdDesc(
			UUID organizationId, UUID employeeId, UUID trainingId);
	List<TrainingCompletion> findAllByOrganizationIdAndEmployeeIdOrderByCompletedAtDescIdDesc(UUID organizationId, UUID employeeId);

	@Modifying
	@Query(value = """
			insert into training_completions (id, organization_id, employee_id, training_id, training_version_id,
			 source_assignment_id, completion_date, completed_at, completion_form, final_score,
			 applied_validity_type, applied_validity_value, expiration_date, created_at)
			values (:id, :organizationId, :employeeId, :trainingId, :trainingVersionId, :assignmentId,
			 :completionDate, :completedAt, 'AUTOMATIC', :finalScore, :validityType,
			 :validityValue, :expirationDate, :completedAt)
			on conflict do nothing
			""", nativeQuery = true)
	int insertAutomaticIfAbsent(UUID id, UUID organizationId, UUID employeeId, UUID trainingId,
			UUID trainingVersionId, UUID assignmentId, LocalDate completionDate, Instant completedAt,
			BigDecimal finalScore, String validityType, Integer validityValue, LocalDate expirationDate);
}
