package dev.igorbarbosa.worktrainingsystem.notifications.api;

import dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery.Status;
import java.time.Instant;
import java.util.UUID;

public record EmailDeliveryResponse(UUID id, UUID notificationId, String recipient, String subject, Status status,
		int attemptCount, String lastError, Instant createdAt, Instant updatedAt) {}
