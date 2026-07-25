package dev.igorbarbosa.worktrainingsystem.assignments.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_assignments")
public class TrainingAssignment extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "training_id", nullable = false, updatable = false) private UUID trainingId;
	@Column(name = "training_version_id", nullable = false, updatable = false) private UUID trainingVersionId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 32) private AssignmentOrigin origin;
	@Column(name = "assigned_at", nullable = false, updatable = false) private Instant assignedAt;
	@Column(name = "assigned_date", nullable = false, updatable = false) private LocalDate assignedDate;
	@Column(name = "due_date") private LocalDate dueDate;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AssignmentStatus status;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AssignmentPriority priority;
	@Column(name = "responsible_user_id", nullable = false, updatable = false) private UUID responsibleUserId;
	@Column(nullable = false, updatable = false) private boolean recertification;
	@Column(name = "recertification_of_assignment_id", updatable = false) private UUID recertificationOfAssignmentId;
	@Column(name = "recertification_of_completion_id", updatable = false) private UUID recertificationOfCompletionId;
	@Column(name = "cancelled_at") private Instant cancelledAt;
	@Column(name = "cancelled_by_user_id") private UUID cancelledByUserId;
	@Column(name = "cancellation_reason", length = 1000) private String cancellationReason;
	@Column(name = "waived_at") private Instant waivedAt;
	@Column(name = "waived_by_user_id") private UUID waivedByUserId;
	@Column(name = "waiver_reason", length = 1000) private String waiverReason;
	@Column(name = "idempotency_key", updatable = false, length = 200) private String idempotencyKey;
	@Column(name = "request_hash", updatable = false, length = 64) private String requestHash;
	@Column(name = "batch_id", updatable = false) private UUID batchId;

	protected TrainingAssignment() {}

	public TrainingAssignment(UUID organizationId, UUID employeeId, UUID trainingId, UUID trainingVersionId,
			AssignmentOrigin origin, Instant assignedAt, LocalDate dueDate, AssignmentPriority priority,
			UUID responsibleUserId, UUID recertificationOfAssignmentId, String idempotencyKey,
			String requestHash, UUID batchId) {
		this.organizationId = organizationId;
		this.employeeId = employeeId;
		this.trainingId = trainingId;
		this.trainingVersionId = trainingVersionId;
		this.origin = origin;
		this.assignedAt = assignedAt;
		this.assignedDate = assignedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
		this.dueDate = dueDate;
		this.status = AssignmentStatus.NOT_STARTED;
		this.priority = priority;
		this.responsibleUserId = responsibleUserId;
		this.recertification = origin == AssignmentOrigin.RECERTIFICATION;
		this.recertificationOfAssignmentId = recertificationOfAssignmentId;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.batchId = batchId;
	}

	public void update(LocalDate dueDate, AssignmentPriority priority) {
		if (status.isTerminal()) throw new IllegalStateException("terminal");
		this.dueDate = dueDate;
		this.priority = priority;
	}
	public void cancel(UUID actor, String reason, Instant now) {
		if (!status.canCloseAdministratively()) throw new IllegalStateException("transition");
		status = AssignmentStatus.CANCELLED; cancelledAt = now; cancelledByUserId = actor; cancellationReason = reason;
	}
	public void waive(UUID actor, String reason, Instant now) {
		if (!status.canCloseAdministratively()) throw new IllegalStateException("transition");
		status = AssignmentStatus.WAIVED; waivedAt = now; waivedByUserId = actor; waiverReason = reason;
	}
	public void start() {
		if (status != AssignmentStatus.NOT_STARTED) throw new IllegalStateException("transition");
		status = AssignmentStatus.IN_PROGRESS;
	}
	public void awaitAssessment() {
		if (status != AssignmentStatus.IN_PROGRESS) throw new IllegalStateException("transition");
		status = AssignmentStatus.AWAITING_ASSESSMENT;
	}
	public void assessmentResult(boolean approved, boolean allQuestionnairesPassed) {
		if (status != AssignmentStatus.AWAITING_ASSESSMENT && status != AssignmentStatus.FAILED)
			throw new IllegalStateException("transition");
		status = approved ? (allQuestionnairesPassed ? AssignmentStatus.APPROVED : AssignmentStatus.AWAITING_ASSESSMENT)
				: AssignmentStatus.FAILED;
	}
	public void complete() {
		if (status != AssignmentStatus.IN_PROGRESS && status != AssignmentStatus.AWAITING_ASSESSMENT
				&& status != AssignmentStatus.APPROVED && status != AssignmentStatus.FAILED)
			throw new IllegalStateException("transition");
		status = AssignmentStatus.COMPLETED;
	}

	public UUID getOrganizationId() { return organizationId; }
	public UUID getEmployeeId() { return employeeId; }
	public UUID getTrainingId() { return trainingId; }
	public UUID getTrainingVersionId() { return trainingVersionId; }
	public AssignmentOrigin getOrigin() { return origin; }
	public Instant getAssignedAt() { return assignedAt; }
	public LocalDate getAssignedDate() { return assignedDate; }
	public LocalDate getDueDate() { return dueDate; }
	public AssignmentStatus getStatus() { return status; }
	public AssignmentPriority getPriority() { return priority; }
	public UUID getResponsibleUserId() { return responsibleUserId; }
	public boolean isRecertification() { return recertification; }
	public UUID getRecertificationOfAssignmentId() { return recertificationOfAssignmentId; }
	public UUID getRecertificationOfCompletionId() { return recertificationOfCompletionId; }
	public Instant getCancelledAt() { return cancelledAt; }
	public UUID getCancelledByUserId() { return cancelledByUserId; }
	public String getCancellationReason() { return cancellationReason; }
	public Instant getWaivedAt() { return waivedAt; }
	public UUID getWaivedByUserId() { return waivedByUserId; }
	public String getWaiverReason() { return waiverReason; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getRequestHash() { return requestHash; }
	public UUID getBatchId() { return batchId; }
}
