package dev.igorbarbosa.worktrainingsystem.assessments.persistence;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentAttempt;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID>,
		JpaSpecificationExecutor<AssessmentAttempt> {
	Optional<AssessmentAttempt> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<AssessmentAttempt> findByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndIdempotencyKey(
			UUID organizationId, UUID assignmentId, UUID questionnaireId, String idempotencyKey);
	Optional<AssessmentAttempt> findFirstByOrganizationIdAndAssignmentIdAndQuestionnaireIdOrderByAttemptNumberDesc(
			UUID organizationId, UUID assignmentId, UUID questionnaireId);
	Optional<AssessmentAttempt> findFirstByOrganizationIdAndEmployeeIdAndTrainingIdOrderBySubmittedAtDescIdDesc(
			UUID organizationId, UUID employeeId, UUID trainingId);
	boolean existsByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndResult(UUID organizationId,
			UUID assignmentId, UUID questionnaireId, AssessmentResult result);
	List<AssessmentAttempt> findAllByOrganizationIdAndAssignmentIdAndQuestionnaireIdInAndResult(
			UUID organizationId, UUID assignmentId, Collection<UUID> questionnaireIds, AssessmentResult result);
	Page<AssessmentAttempt> findAllByOrganizationIdAndAssignmentId(UUID organizationId, UUID assignmentId, Pageable pageable);
}
