package dev.igorbarbosa.worktrainingsystem.certificates.persistence;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateGenerationJob;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateGenerationStatus;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateGenerationJobRepository extends JpaRepository<CertificateGenerationJob, UUID> {
	Optional<CertificateGenerationJob> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<CertificateGenerationJob> findFirstByOrganizationIdAndCompletionIdAndCertificateTypeAndStatusInOrderByCreatedAtDesc(
			UUID organizationId, UUID completionId, CertificateType type, Collection<CertificateGenerationStatus> statuses);
}
