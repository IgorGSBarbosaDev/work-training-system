package dev.igorbarbosa.worktrainingsystem.progress.persistence;

import dev.igorbarbosa.worktrainingsystem.progress.domain.VideoProgress;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VideoProgressRepository extends JpaRepository<VideoProgress, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<VideoProgress> findByOrganizationIdAndAssignmentIdAndVideoId(UUID organizationId, UUID assignmentId, UUID videoId);
	Optional<VideoProgress> findFirstByOrganizationIdAndAssignmentIdOrderByUpdatedAtDesc(UUID organizationId, UUID assignmentId);
	List<VideoProgress> findAllByOrganizationIdAndAssignmentId(UUID organizationId, UUID assignmentId);
}
