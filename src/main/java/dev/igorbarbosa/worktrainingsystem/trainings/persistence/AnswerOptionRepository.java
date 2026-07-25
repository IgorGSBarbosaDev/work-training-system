package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.AnswerOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, UUID> {

	List<AnswerOption> findAllByQuestionIdOrderByDisplayOrder(UUID questionId);

	Optional<AnswerOption> findByIdAndQuestionId(UUID id, UUID questionId);

	long countByQuestionIdAndStatus(UUID questionId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status);

	long countByQuestionIdAndStatusAndCorrect(UUID questionId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status, boolean correct);

	long countByQuestionIdAndStatusAndCorrectAndIdNot(UUID questionId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status, boolean correct, UUID id);
}
