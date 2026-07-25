package dev.igorbarbosa.worktrainingsystem.progress.persistence;

import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgressEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoProgressEventRepository extends JpaRepository<VideoProgressEvent, UUID> {
	Optional<VideoProgressEvent> findByOrganizationIdAndAssignmentIdAndVideoIdAndEventIdentifier(
			UUID organizationId, UUID assignmentId, UUID videoId, String eventIdentifier);
}
