package dev.igorbarbosa.worktrainingsystem.assignments.persistence;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentSource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface AssignmentSourceRepository extends JpaRepository<AssignmentSource, UUID> {
	boolean existsByOrganizationIdAndAssignmentIdAndOriginAndSourceReferenceId(
			UUID organizationId, UUID assignmentId, AssignmentOrigin origin, UUID sourceReferenceId);
	List<AssignmentSource> findAllByOrganizationIdAndAssignmentIdOrderByCreatedAt(
			UUID organizationId, UUID assignmentId);
	@Modifying
	@Query(value = """
			insert into assignment_sources (id, organization_id, assignment_id, origin, source_reference_id, created_at)
			values (:id, :organizationId, :assignmentId, :origin, :sourceReferenceId, :createdAt)
			on conflict do nothing
			""", nativeQuery = true)
	int insertIfAbsent(UUID id, UUID organizationId, UUID assignmentId, String origin,
			UUID sourceReferenceId, Instant createdAt);
}
