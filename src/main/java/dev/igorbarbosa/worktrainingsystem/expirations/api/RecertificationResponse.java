package dev.igorbarbosa.worktrainingsystem.expirations.api;

import dev.igorbarbosa.worktrainingsystem.expirations.domain.RecertificationTrigger;
import java.time.Instant;
import java.util.UUID;

public record RecertificationResponse(UUID id, UUID completionId, UUID assignmentId,
		RecertificationTrigger triggerType, UUID responsibleUserId, Instant createdAt) {}
