package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingModule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingModuleRepository extends JpaRepository<TrainingModule, UUID> {

	List<TrainingModule> findAllByTrainingVersionIdOrderByDisplayOrder(UUID trainingVersionId);

	Optional<TrainingModule> findByIdAndTrainingVersionId(UUID id, UUID trainingVersionId);

	long countByTrainingVersionIdAndStatus(UUID trainingVersionId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status);
}
