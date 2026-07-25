package dev.igorbarbosa.worktrainingsystem.assessments.domain;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_completions")
public class TrainingCompletion {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "training_id", nullable = false, updatable = false) private UUID trainingId;
	@Column(name = "training_version_id", nullable = false, updatable = false) private UUID trainingVersionId;
	@Column(name = "source_assignment_id", updatable = false) private UUID sourceAssignmentId;
	@Column(name = "completion_date", nullable = false, updatable = false) private LocalDate completionDate;
	@Column(name = "completed_at", nullable = false, updatable = false) private Instant completedAt;
	@Enumerated(EnumType.STRING) @Column(name = "completion_form", nullable = false, updatable = false, length = 16) private CompletionForm completionForm;
	@Column(name = "final_score", updatable = false, precision = 5, scale = 2) private BigDecimal finalScore;
	@Enumerated(EnumType.STRING) @Column(name = "applied_validity_type", nullable = false, updatable = false, length = 16) private ValidityType appliedValidityType;
	@Column(name = "applied_validity_value", updatable = false) private Integer appliedValidityValue;
	@Column(name = "expiration_date", updatable = false) private LocalDate expirationDate;
	@Column(name = "responsible_user_id", updatable = false) private UUID responsibleUserId;
	@Column(updatable = false, length = 2000) private String notes;
	@Column(name = "external_evidence_file_id", updatable = false) private UUID externalEvidenceFileId;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected TrainingCompletion() {}
	public TrainingCompletion(UUID organizationId, UUID employeeId, UUID trainingId, UUID trainingVersionId,
			UUID sourceAssignmentId, Instant completedAt, CompletionForm form, BigDecimal finalScore,
			ValidityType validityType, Integer validityValue, LocalDate expirationDate,
			UUID responsibleUserId, String notes, UUID evidenceFileId) {
		this.organizationId = organizationId; this.employeeId = employeeId; this.trainingId = trainingId;
		this.trainingVersionId = trainingVersionId; this.sourceAssignmentId = sourceAssignmentId;
		this.completedAt = completedAt; this.completionDate = completedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
		this.completionForm = form; this.finalScore = finalScore; this.appliedValidityType = validityType;
		this.appliedValidityValue = validityValue; this.expirationDate = expirationDate;
		this.responsibleUserId = responsibleUserId; this.notes = notes; this.externalEvidenceFileId = evidenceFileId;
		this.createdAt = completedAt;
	}
	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public UUID getEmployeeId() { return employeeId; }
	public UUID getTrainingId() { return trainingId; }
	public UUID getTrainingVersionId() { return trainingVersionId; }
	public UUID getSourceAssignmentId() { return sourceAssignmentId; }
	public LocalDate getCompletionDate() { return completionDate; }
	public Instant getCompletedAt() { return completedAt; }
	public CompletionForm getCompletionForm() { return completionForm; }
	public BigDecimal getFinalScore() { return finalScore; }
	public ValidityType getAppliedValidityType() { return appliedValidityType; }
	public Integer getAppliedValidityValue() { return appliedValidityValue; }
	public LocalDate getExpirationDate() { return expirationDate; }
	public UUID getResponsibleUserId() { return responsibleUserId; }
	public String getNotes() { return notes; }
	public UUID getExternalEvidenceFileId() { return externalEvidenceFileId; }
}
