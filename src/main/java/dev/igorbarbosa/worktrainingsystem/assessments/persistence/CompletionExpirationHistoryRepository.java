package dev.igorbarbosa.worktrainingsystem.assessments.persistence;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionExpirationHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletionExpirationHistoryRepository extends JpaRepository<CompletionExpirationHistory, UUID> {
	Optional<CompletionExpirationHistory> findFirstByOrganizationIdAndCompletionIdOrderByCreatedAtDescIdDesc(
			UUID organizationId, UUID completionId);
}
