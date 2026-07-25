package dev.igorbarbosa.worktrainingsystem.assignments.persistence;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentBatchResultRepository extends JpaRepository<AssignmentBatchResult, UUID> {
	List<AssignmentBatchResult> findAllByOrganizationIdAndBatchIdOrderByEmployeeId(UUID organizationId, UUID batchId);
}
