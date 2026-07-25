package dev.igorbarbosa.worktrainingsystem.qrverification.persistence;

import dev.igorbarbosa.worktrainingsystem.qrverification.domain.QrCodeAccessLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCodeAccessLogRepository extends JpaRepository<QrCodeAccessLog, UUID> {
	Page<QrCodeAccessLog> findAllByOrganizationIdAndQrCodeIdOrderByQueriedAtDesc(UUID organizationId, UUID qrCodeId, Pageable pageable);
}
