package dev.igorbarbosa.worktrainingsystem.qrverification.persistence;

import dev.igorbarbosa.worktrainingsystem.qrverification.domain.EmployeeQrCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeQrCodeRepository extends JpaRepository<EmployeeQrCode, UUID> {
	Optional<EmployeeQrCode> findByOrganizationIdAndEmployeeIdAndStatus(UUID organizationId, UUID employeeId, EmployeeQrCode.Status status);
	Optional<EmployeeQrCode> findByTokenHash(String tokenHash);
	Optional<EmployeeQrCode> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
