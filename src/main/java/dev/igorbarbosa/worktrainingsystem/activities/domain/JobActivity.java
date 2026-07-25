package dev.igorbarbosa.worktrainingsystem.activities.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_activities")
public class JobActivity extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "job_id", nullable = false, updatable = false) private UUID jobId;
	@Column(name = "activity_id", nullable = false, updatable = false) private UUID activityId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private RegistrationStatus status;
	@Column(name = "linked_at", nullable = false, updatable = false) private Instant linkedAt;
	@Column(name = "unlinked_at") private Instant unlinkedAt;
	@Column(name = "linked_by_user_id", nullable = false, updatable = false) private UUID linkedByUserId;
	@Column(name = "unlinked_by_user_id") private UUID unlinkedByUserId;

	protected JobActivity() {}
	public JobActivity(UUID organizationId, UUID jobId, UUID activityId, UUID actor, Instant now) {
		this.organizationId = organizationId; this.jobId = jobId; this.activityId = activityId;
		this.status = RegistrationStatus.ACTIVE; this.linkedByUserId = actor; this.linkedAt = now;
	}
	public UUID getOrganizationId() { return organizationId; }
	public UUID getJobId() { return jobId; }
	public UUID getActivityId() { return activityId; }
	public RegistrationStatus getStatus() { return status; }
	public Instant getLinkedAt() { return linkedAt; }
	public Instant getUnlinkedAt() { return unlinkedAt; }
	public UUID getLinkedByUserId() { return linkedByUserId; }
	public void deactivate(UUID actor, Instant now) {
		if (status == RegistrationStatus.INACTIVE) return;
		status = RegistrationStatus.INACTIVE; unlinkedAt = now; unlinkedByUserId = actor;
	}
}
