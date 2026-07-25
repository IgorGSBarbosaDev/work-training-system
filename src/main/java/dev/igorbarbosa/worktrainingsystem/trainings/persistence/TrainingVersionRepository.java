package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingVersionRepository extends JpaRepository<TrainingVersion, UUID> {

	List<TrainingVersion> findAllByTrainingIdOrderByVersionNumberDesc(UUID trainingId);

	Optional<TrainingVersion> findByIdAndTrainingId(UUID id, UUID trainingId);

	Optional<TrainingVersion> findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(
			UUID trainingId, TrainingVersionStatus status);

	boolean existsByTrainingIdAndVersionNumber(UUID trainingId, int versionNumber);

	int countByTrainingId(UUID trainingId);

	Optional<TrainingVersion> findFirstByTrainingIdAndStatusOrderByVersionNumberAsc(
			UUID trainingId, TrainingVersionStatus status);

	@org.springframework.data.jpa.repository.Query("select coalesce(max(v.versionNumber), 0) from TrainingVersion v where v.trainingId = :trainingId")
	int findMaximumVersionNumber(UUID trainingId);

}
