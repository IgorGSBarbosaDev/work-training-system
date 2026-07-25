package dev.igorbarbosa.worktrainingsystem.notifications.persistence;

import dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {
	Page<EmailDelivery> findAllByOrganizationId(UUID organizationId, Pageable pageable);
	java.util.Optional<EmailDelivery> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
