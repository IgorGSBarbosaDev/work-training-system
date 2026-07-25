package dev.igorbarbosa.worktrainingsystem.expirations.persistence;

import dev.igorbarbosa.worktrainingsystem.expirations.domain.CompletionExpirationState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletionExpirationStateRepository extends JpaRepository<CompletionExpirationState, CompletionExpirationState.Key> {
	Optional<CompletionExpirationState> findByCompletionIdAndOrganizationId(UUID completionId, UUID organizationId);
}
