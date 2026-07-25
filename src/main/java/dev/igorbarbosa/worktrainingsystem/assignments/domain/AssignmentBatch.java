package dev.igorbarbosa.worktrainingsystem.assignments.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "assignment_batches")
public class AssignmentBatch extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "requested_by_user_id", nullable = false, updatable = false) private UUID requestedByUserId;
	@Column(name = "idempotency_key", updatable = false, length = 200) private String idempotencyKey;
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64) private String requestHash;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AssignmentBatchStatus status;
	@Column(name = "requested_count", nullable = false) private int requestedCount;
	@Column(name = "created_count", nullable = false) private int createdCount;
	@Column(name = "skipped_count", nullable = false) private int skippedCount;
	@Column(name = "failed_count", nullable = false) private int failedCount;
	protected AssignmentBatch() {}
	public AssignmentBatch(UUID organizationId, UUID actor, String key, String hash) {
		this.organizationId = organizationId; requestedByUserId = actor; idempotencyKey = key;
		requestHash = hash; status = AssignmentBatchStatus.PROCESSING;
	}
	public void complete(int requested, int created, int skipped, int failed) {
		requestedCount = requested; createdCount = created; skippedCount = skipped; failedCount = failed;
		status = AssignmentBatchStatus.COMPLETED;
	}
	public UUID getOrganizationId() { return organizationId; }
	public UUID getRequestedByUserId() { return requestedByUserId; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getRequestHash() { return requestHash; }
	public AssignmentBatchStatus getStatus() { return status; }
	public int getRequestedCount() { return requestedCount; }
	public int getCreatedCount() { return createdCount; }
	public int getSkippedCount() { return skippedCount; }
	public int getFailedCount() { return failedCount; }
}
