package dev.igorbarbosa.worktrainingsystem.identity.api;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID userId, String action, String entityType, UUID entityId,
		Instant occurredAt, String requestId, String details) {}
