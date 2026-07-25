package dev.igorbarbosa.worktrainingsystem.notifications.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String message, String relatedEntityType,
		UUID relatedEntityId, Instant createdAt, Instant readAt, Instant archivedAt) {}
