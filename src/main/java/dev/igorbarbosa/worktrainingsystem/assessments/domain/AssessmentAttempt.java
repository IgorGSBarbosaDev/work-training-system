package dev.igorbarbosa.worktrainingsystem.assessments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_attempts")
public class AssessmentAttempt {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "training_id", nullable = false, updatable = false) private UUID trainingId;
	@Column(name = "training_version_id", nullable = false, updatable = false) private UUID trainingVersionId;
	@Column(name = "questionnaire_id", nullable = false, updatable = false) private UUID questionnaireId;
	@Column(name = "attempt_number", nullable = false, updatable = false) private int attemptNumber;
	@Column(name = "submitted_at", nullable = false, updatable = false) private Instant submittedAt;
	@Column(nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal score;
	@Column(name = "passing_score", nullable = false, updatable = false, precision = 5, scale = 2) private BigDecimal passingScore;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 16) private AssessmentResult result;
	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 200) private String idempotencyKey;
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64) private String requestHash;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

	protected AssessmentAttempt() {}
	public AssessmentAttempt(UUID organizationId, UUID assignmentId, UUID employeeId, UUID trainingId,
			UUID trainingVersionId, UUID questionnaireId, int attemptNumber, Instant submittedAt,
			BigDecimal score, BigDecimal passingScore, AssessmentResult result, String idempotencyKey, String requestHash) {
		this.organizationId = organizationId; this.assignmentId = assignmentId; this.employeeId = employeeId;
		this.trainingId = trainingId; this.trainingVersionId = trainingVersionId; this.questionnaireId = questionnaireId;
		this.attemptNumber = attemptNumber; this.submittedAt = submittedAt; this.score = score;
		this.passingScore = passingScore; this.result = result; this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash; this.createdAt = submittedAt;
	}
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getAssignmentId() { return assignmentId; }
	public UUID getEmployeeId() { return employeeId; }
	public UUID getTrainingId() { return trainingId; }
	public UUID getTrainingVersionId() { return trainingVersionId; }
	public UUID getQuestionnaireId() { return questionnaireId; }
	public int getAttemptNumber() { return attemptNumber; }
	public Instant getSubmittedAt() { return submittedAt; }
	public BigDecimal getScore() { return score; }
	public BigDecimal getPassingScore() { return passingScore; }
	public AssessmentResult getResult() { return result; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getRequestHash() { return requestHash; }
}
