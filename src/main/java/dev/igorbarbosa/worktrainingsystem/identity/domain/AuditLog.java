package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.identity.application.AuditPort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
	@Id private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "user_id", updatable = false) private UUID userId;
	@Column(nullable = false, updatable = false, length = 100) private String action;
	@Column(name = "entity_type", nullable = false, updatable = false, length = 100) private String entityType;
	@Column(name = "entity_id", updatable = false) private UUID entityId;
	@Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
	@Column(name = "request_id", updatable = false, length = 128) private String requestId;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb") private String details;
	protected AuditLog() {}
	public AuditLog(UUID organizationId, AuditPort.AuditRecord record, String requestId, String details) {
		this.id = UUID.randomUUID(); this.organizationId = organizationId; this.userId = record.actorId();
		this.action = record.action(); this.entityType = record.entityType(); this.entityId = record.entityId();
		this.occurredAt = record.occurredAt(); this.requestId = requestId; this.details = details;
	}
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getUserId() { return userId; }
	public String getAction() { return action; }
	public String getEntityType() { return entityType; }
	public UUID getEntityId() { return entityId; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getRequestId() { return requestId; }
	public String getDetails() { return details; }
}
