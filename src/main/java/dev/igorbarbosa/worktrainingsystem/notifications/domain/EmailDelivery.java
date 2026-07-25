package dev.igorbarbosa.worktrainingsystem.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "email_deliveries")
public class EmailDelivery {
	public enum Status { PENDING, SENT, FAILED }
	@Id private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
	@Column(name = "notification_id", updatable = false) private UUID notificationId;
	@Column(nullable = false, updatable = false, length = 254) private String recipient;
	@Column(nullable = false, updatable = false, length = 200) private String subject;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
	@Column(name = "attempt_count", nullable = false) private int attemptCount;
	@Column(name = "last_error", length = 1000) private String lastError;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected EmailDelivery() {}
	public EmailDelivery(UUID organizationId, UUID userId, UUID notificationId, String recipient, String subject, Instant now) {
		this.id = UUID.randomUUID(); this.organizationId = organizationId; this.userId = userId; this.notificationId = notificationId;
		this.recipient = recipient; this.subject = subject; this.status = Status.PENDING; this.createdAt = now; this.updatedAt = now;
	}
	public void retry(Instant now) { status = Status.PENDING; lastError = null; updatedAt = now; }
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getUserId() { return userId; }
	public UUID getNotificationId() { return notificationId; }
	public String getRecipient() { return recipient; }
	public String getSubject() { return subject; }
	public Status getStatus() { return status; }
	public int getAttemptCount() { return attemptCount; }
	public String getLastError() { return lastError; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
