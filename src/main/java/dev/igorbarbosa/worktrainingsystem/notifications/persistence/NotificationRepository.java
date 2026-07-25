package dev.igorbarbosa.worktrainingsystem.notifications.persistence;

import dev.igorbarbosa.worktrainingsystem.notifications.domain.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	Page<Notification> findAllByOrganizationIdAndUserIdAndArchivedAtIsNull(UUID organizationId, UUID userId, Pageable pageable);
	long countByOrganizationIdAndUserIdAndReadAtIsNullAndArchivedAtIsNull(UUID organizationId, UUID userId);
}
