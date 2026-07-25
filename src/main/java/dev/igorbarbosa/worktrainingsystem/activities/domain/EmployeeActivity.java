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
@Table(name = "employee_activities")
public class EmployeeActivity extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "activity_id", nullable = false, updatable = false) private UUID activityId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 16) private EmployeeActivityOrigin origin;
	@Column(name = "source_job_activity_id", updatable = false) private UUID sourceJobActivityId;
	@Column(length = 1000, updatable = false) private String reason;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private RegistrationStatus status;
	@Column(name = "assigned_at", nullable = false, updatable = false) private Instant assignedAt;
	@Column(name = "deactivated_at") private Instant deactivatedAt;
	@Column(name = "assigned_by_user_id", nullable = false, updatable = false) private UUID assignedByUserId;
	@Column(name = "deactivated_by_user_id") private UUID deactivatedByUserId;

	protected EmployeeActivity() {}
	public EmployeeActivity(UUID organizationId, UUID employeeId, UUID activityId, EmployeeActivityOrigin origin,
			UUID sourceJobActivityId, String reason, UUID actor, Instant now) {
		this.organizationId = organizationId; this.employeeId = employeeId; this.activityId = activityId;
		this.origin = origin; this.sourceJobActivityId = sourceJobActivityId; this.reason = reason;
		this.status = RegistrationStatus.ACTIVE; this.assignedByUserId = actor; this.assignedAt = now;
	}
	public UUID getOrganizationId() { return organizationId; }
	public UUID getEmployeeId() { return employeeId; }
	public UUID getActivityId() { return activityId; }
	public EmployeeActivityOrigin getOrigin() { return origin; }
	public UUID getSourceJobActivityId() { return sourceJobActivityId; }
	public String getReason() { return reason; }
	public RegistrationStatus getStatus() { return status; }
	public Instant getAssignedAt() { return assignedAt; }
	public void deactivate(UUID actor, Instant now) {
		if (status == RegistrationStatus.INACTIVE) return;
		status = RegistrationStatus.INACTIVE; deactivatedAt = now; deactivatedByUserId = actor;
	}
}
