package dev.igorbarbosa.worktrainingsystem.identity.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface AuditPort {
	void record(AuditRecord record);

	record AuditRecord(UUID actorId, String action, String entityType, UUID entityId,
			Instant occurredAt, Map<String, String> details) {}
}
