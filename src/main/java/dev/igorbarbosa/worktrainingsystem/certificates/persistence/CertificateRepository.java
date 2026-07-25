package dev.igorbarbosa.worktrainingsystem.certificates.persistence;

import dev.igorbarbosa.worktrainingsystem.certificates.domain.Certificate;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateStatus;
import dev.igorbarbosa.worktrainingsystem.certificates.domain.CertificateType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CertificateRepository extends JpaRepository<Certificate, UUID>, JpaSpecificationExecutor<Certificate> {
	Optional<Certificate> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<Certificate> findByOrganizationIdAndCompletionIdAndTypeAndStatus(UUID organizationId, UUID completionId,
			CertificateType type, CertificateStatus status);
	Optional<Certificate> findFirstByOrganizationIdAndCompletionIdAndStatusOrderByIssuedAtDesc(UUID organizationId,
			UUID completionId, CertificateStatus status);
	Optional<Certificate> findByValidationCode(String validationCode);
	int countByOrganizationIdAndCompletionIdAndType(UUID organizationId, UUID completionId, CertificateType type);
	java.util.List<Certificate> findAllByOrganizationIdAndCompletionIdOrderByIssuedAtDesc(UUID organizationId, UUID completionId);
}
