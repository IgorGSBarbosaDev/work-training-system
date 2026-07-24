package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.Video;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, UUID> {

	List<Video> findAllByModuleIdOrderByDisplayOrder(UUID moduleId);

	Optional<Video> findByIdAndModuleId(UUID id, UUID moduleId);

	long countByModuleIdAndStatus(UUID moduleId,
			dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus status);
}
