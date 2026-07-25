package dev.igorbarbosa.worktrainingsystem.assignments.persistence;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatch;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentBatchRepository extends JpaRepository<AssignmentBatch, UUID> {
	Optional<AssignmentBatch> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<AssignmentBatch> findByOrganizationIdAndRequestedByUserIdAndIdempotencyKey(
			UUID organizationId, UUID requestedByUserId, String idempotencyKey);
}
