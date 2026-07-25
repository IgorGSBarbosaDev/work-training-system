package dev.igorbarbosa.worktrainingsystem.assignments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "assignment_batch_results")
@EntityListeners(AuditingEntityListener.class)
public class AssignmentBatchResult {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "batch_id", nullable = false, updatable = false) private UUID batchId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 16) private AssignmentBatchResultType result;
	@Column(name = "assignment_id", updatable = false) private UUID assignmentId;
	@Column(updatable = false, length = 64) private String code;
	@Column(updatable = false, length = 1000) private String message;
	@CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected AssignmentBatchResult() {}
	public AssignmentBatchResult(UUID organizationId, UUID batchId, UUID employeeId,
			AssignmentBatchResultType result, UUID assignmentId, String code, String message) {
		this.organizationId = organizationId; this.batchId = batchId; this.employeeId = employeeId;
		this.result = result; this.assignmentId = assignmentId; this.code = code; this.message = message;
	}
	public UUID getEmployeeId() { return employeeId; }
	public AssignmentBatchResultType getResult() { return result; }
	public UUID getAssignmentId() { return assignmentId; }
	public String getCode() { return code; }
	public String getMessage() { return message; }
}
