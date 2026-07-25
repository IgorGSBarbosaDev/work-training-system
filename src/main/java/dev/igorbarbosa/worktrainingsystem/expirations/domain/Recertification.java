package dev.igorbarbosa.worktrainingsystem.expirations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "recertifications")
public class Recertification {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "completion_id", nullable = false, updatable = false) private UUID completionId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Enumerated(EnumType.STRING) @Column(name = "trigger_type", nullable = false, updatable = false) private RecertificationTrigger triggerType;
	@Column(name = "responsible_user_id", nullable = false, updatable = false) private UUID responsibleUserId;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected Recertification() {}
	public Recertification(UUID organizationId, UUID completionId, UUID assignmentId,
			RecertificationTrigger triggerType, UUID responsibleUserId, Instant createdAt) {
		this.organizationId = organizationId; this.completionId = completionId; this.assignmentId = assignmentId;
		this.triggerType = triggerType; this.responsibleUserId = responsibleUserId; this.createdAt = createdAt;
	}
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getCompletionId() { return completionId; }
	public UUID getAssignmentId() { return assignmentId; }
	public RecertificationTrigger getTriggerType() { return triggerType; }
	public UUID getResponsibleUserId() { return responsibleUserId; }
	public Instant getCreatedAt() { return createdAt; }
}
