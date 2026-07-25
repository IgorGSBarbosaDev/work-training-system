package dev.igorbarbosa.worktrainingsystem.expirations.persistence;

import dev.igorbarbosa.worktrainingsystem.expirations.domain.CompletionExpirationStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletionExpirationStatusHistoryRepository extends JpaRepository<CompletionExpirationStatusHistory, UUID> {}
