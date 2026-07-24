package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.Questionnaire;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {

	Optional<Questionnaire> findByIdAndModuleId(UUID id, UUID moduleId);

	Optional<Questionnaire> findByModuleId(UUID moduleId);
}
