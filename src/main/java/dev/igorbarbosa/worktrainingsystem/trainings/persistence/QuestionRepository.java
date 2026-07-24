package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.Question;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

	List<Question> findAllByQuestionnaireIdOrderByDisplayOrder(UUID questionnaireId);

	Optional<Question> findByIdAndQuestionnaireId(UUID id, UUID questionnaireId);

	long countByQuestionnaireIdAndStatus(UUID questionnaireId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status);
}
