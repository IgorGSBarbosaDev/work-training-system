package dev.igorbarbosa.worktrainingsystem.certificates.persistence;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateHistoryRepository extends JpaRepository<CertificateHistory, UUID> {}
