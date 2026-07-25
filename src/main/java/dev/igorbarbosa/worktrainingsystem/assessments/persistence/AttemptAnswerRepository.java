package dev.igorbarbosa.worktrainingsystem.assessments.persistence;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AttemptAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
	List<AttemptAnswer> findAllByOrganizationIdAndAttemptId(UUID organizationId, UUID attemptId);
}
