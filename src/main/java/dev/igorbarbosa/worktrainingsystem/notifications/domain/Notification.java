package dev.igorbarbosa.worktrainingsystem.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "notifications")
public class Notification {
	@Id private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
	@Column(nullable = false, updatable = false, length = 64) private String type;
	@Column(nullable = false, updatable = false, length = 200) private String title;
	@Column(nullable = false, updatable = false, length = 2000) private String message;
	@Column(name = "related_entity_type", updatable = false) private String relatedEntityType;
	@Column(name = "related_entity_id", updatable = false) private UUID relatedEntityId;
	@Column(name = "deduplication_key", nullable = false, updatable = false, length = 240) private String deduplicationKey;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "read_at") private Instant readAt;
	@Column(name = "archived_at") private Instant archivedAt;
	protected Notification() {}
	public Notification(UUID organizationId, UUID userId, String type, String title, String message,
			String relatedEntityType, UUID relatedEntityId, String deduplicationKey, Instant createdAt) {
		this.id = UUID.randomUUID(); this.organizationId = organizationId; this.userId = userId; this.type = type;
		this.title = title; this.message = message; this.relatedEntityType = relatedEntityType;
		this.relatedEntityId = relatedEntityId; this.deduplicationKey = deduplicationKey; this.createdAt = createdAt;
	}
	public void read(Instant now) { if (readAt == null) readAt = now; }
	public void archive(Instant now) { archivedAt = now; }
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getUserId() { return userId; }
	public String getType() { return type; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getRelatedEntityType() { return relatedEntityType; }
	public UUID getRelatedEntityId() { return relatedEntityId; }
	public String getDeduplicationKey() { return deduplicationKey; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getReadAt() { return readAt; }
	public Instant getArchivedAt() { return archivedAt; }
}
