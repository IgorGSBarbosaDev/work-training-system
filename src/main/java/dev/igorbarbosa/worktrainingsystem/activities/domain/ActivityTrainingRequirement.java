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
@Table(name = "activity_training_requirements")
public class ActivityTrainingRequirement extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "activity_id", nullable = false, updatable = false) private UUID activityId;
	@Column(name = "training_id", nullable = false, updatable = false) private UUID trainingId;
	@Enumerated(EnumType.STRING) @Column(name = "version_policy", nullable = false, length = 32) private RequirementVersionPolicy versionPolicy;
	@Column(name = "training_version_id") private UUID trainingVersionId;
	@Column(nullable = false) private boolean required;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private RegistrationStatus status;
	@Column(name = "linked_at", nullable = false, updatable = false) private Instant linkedAt;
	@Column(name = "deactivated_at") private Instant deactivatedAt;
	@Column(name = "linked_by_user_id", nullable = false, updatable = false) private UUID linkedByUserId;
	@Column(name = "deactivated_by_user_id") private UUID deactivatedByUserId;

	protected ActivityTrainingRequirement() {}
	public ActivityTrainingRequirement(UUID organizationId, UUID activityId, UUID trainingId,
			RequirementVersionPolicy policy, UUID trainingVersionId, UUID actor, Instant now) {
		this.organizationId = organizationId; this.activityId = activityId; this.trainingId = trainingId;
		this.versionPolicy = policy; this.trainingVersionId = trainingVersionId; this.required = true;
		this.status = RegistrationStatus.ACTIVE; this.linkedAt = now; this.linkedByUserId = actor;
	}
	public UUID getOrganizationId() { return organizationId; }
	public UUID getActivityId() { return activityId; }
	public UUID getTrainingId() { return trainingId; }
	public RequirementVersionPolicy getVersionPolicy() { return versionPolicy; }
	public UUID getTrainingVersionId() { return trainingVersionId; }
	public boolean isRequired() { return required; }
	public RegistrationStatus getStatus() { return status; }
	public Instant getLinkedAt() { return linkedAt; }
	public void update(RequirementVersionPolicy policy, UUID versionId) { this.versionPolicy = policy; this.trainingVersionId = versionId; }
	public void deactivate(UUID actor, Instant now) {
		if (status == RegistrationStatus.INACTIVE) return;
		status = RegistrationStatus.INACTIVE; deactivatedAt = now; deactivatedByUserId = actor;
	}
}
