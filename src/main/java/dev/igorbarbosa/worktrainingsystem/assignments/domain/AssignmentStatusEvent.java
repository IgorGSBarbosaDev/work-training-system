package dev.igorbarbosa.worktrainingsystem.assignments.domain;

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

@Entity
@Table(name = "assignment_status_events")
public class AssignmentStatusEvent {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false, updatable = false, length = 32)
	private AssignmentStatus previousStatus;
	@Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, updatable = false, length = 32)
	private AssignmentStatus newStatus;
	@Column(nullable = false, updatable = false, length = 64) private String reason;
	@Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
	protected AssignmentStatusEvent() {}
	public AssignmentStatusEvent(UUID organizationId, UUID assignmentId, AssignmentStatus previousStatus,
			AssignmentStatus newStatus, String reason, Instant occurredAt) {
		this.organizationId = organizationId; this.assignmentId = assignmentId; this.previousStatus = previousStatus;
		this.newStatus = newStatus; this.reason = reason; this.occurredAt = occurredAt;
	}
}
