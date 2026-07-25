package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "training_versions")
public class TrainingVersion extends BaseEntity {

	@Column(name = "training_id", nullable = false, updatable = false)
	private UUID trainingId;

	@Column(name = "version_number", nullable = false, updatable = false)
	private int versionNumber;

	@Column(name = "workload_minutes", nullable = false)
	private int workloadMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "validity_type", nullable = false, length = 16)
	private ValidityType validityType;

	@Column(name = "validity_value")
	private Integer validityValue;

	@Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
	private BigDecimal passingScore;

	@Column(name = "max_attempts")
	private Integer maxAttempts;

	@Column(name = "retry_interval_minutes", nullable = false)
	private int retryIntervalMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TrainingVersionStatus status;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;
	@Column(name = "training_name_snapshot", nullable = false)
	private String trainingNameSnapshot;
	@Column(name = "training_code_snapshot", nullable = false)
	private String trainingCodeSnapshot;
	@Column(name = "training_description_snapshot")
	private String trainingDescriptionSnapshot;
	@Column(name = "training_category_snapshot")
	private String trainingCategorySnapshot;
	@Column(name = "regulatory_standard_snapshot", nullable = false)
	private boolean regulatoryStandardSnapshot;
	@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
	@Column(name = "content_snapshot", columnDefinition = "jsonb")
	private String contentSnapshot;

	protected TrainingVersion() {
	}

	public TrainingVersion(
			UUID trainingId,
			int versionNumber,
			int workloadMinutes,
			ValidityType validityType,
			Integer validityValue,
			BigDecimal passingScore,
			Integer maxAttempts,
			int retryIntervalMinutes) {
		this.trainingId = trainingId;
		this.versionNumber = versionNumber;
		this.workloadMinutes = workloadMinutes;
		this.validityType = validityType;
		this.validityValue = validityValue;
		this.passingScore = passingScore;
		this.maxAttempts = maxAttempts;
		this.retryIntervalMinutes = retryIntervalMinutes;
		this.status = TrainingVersionStatus.DRAFT;
	}

	public TrainingVersion(Training training, int versionNumber, int workloadMinutes, ValidityType validityType,
			Integer validityValue, BigDecimal passingScore, Integer maxAttempts, int retryIntervalMinutes) {
		this(training.getId(), versionNumber, workloadMinutes, validityType, validityValue, passingScore,
				maxAttempts, retryIntervalMinutes);
		this.organizationId = training.getOrganizationId();
		refreshDraftSnapshot(training);
	}

	public UUID getTrainingId() {
		return trainingId;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public int getWorkloadMinutes() {
		return workloadMinutes;
	}

	public ValidityType getValidityType() {
		return validityType;
	}

	public Integer getValidityValue() {
		return validityValue;
	}

	public BigDecimal getPassingScore() {
		return passingScore;
	}

	public Integer getMaxAttempts() {
		return maxAttempts;
	}

	public int getRetryIntervalMinutes() {
		return retryIntervalMinutes;
	}

	public TrainingVersionStatus getStatus() {
		return status;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}
	public UUID getOrganizationId() { return organizationId; }
	public String getTrainingNameSnapshot() { return trainingNameSnapshot; }
	public String getTrainingCodeSnapshot() { return trainingCodeSnapshot; }
	public String getTrainingDescriptionSnapshot() { return trainingDescriptionSnapshot; }
	public String getTrainingCategorySnapshot() { return trainingCategorySnapshot; }
	public boolean isRegulatoryStandardSnapshot() { return regulatoryStandardSnapshot; }
	public String getContentSnapshot() { return contentSnapshot; }

	public void refreshDraftSnapshot(Training training) {
		if (status != TrainingVersionStatus.DRAFT) throw new IllegalStateException("Only draft snapshots can change");
		this.organizationId = training.getOrganizationId();
		this.trainingNameSnapshot = training.getName(); this.trainingCodeSnapshot = training.getCode();
		this.trainingDescriptionSnapshot = training.getDescription(); this.trainingCategorySnapshot = training.getCategory();
		this.regulatoryStandardSnapshot = training.isRegulatoryStandard();
	}

	public void capturePublicationSnapshot(Training training, String contentSnapshot) {
		refreshDraftSnapshot(training);
		this.contentSnapshot = contentSnapshot;
	}

	public void update(
			int workloadMinutes,
			ValidityType validityType,
			Integer validityValue,
			BigDecimal passingScore,
			Integer maxAttempts,
			int retryIntervalMinutes) {
		this.workloadMinutes = workloadMinutes;
		this.validityType = validityType;
		this.validityValue = validityValue;
		this.passingScore = passingScore;
		this.maxAttempts = maxAttempts;
		this.retryIntervalMinutes = retryIntervalMinutes;
	}

	public void publish(Instant publishedAt) {
		this.status = TrainingVersionStatus.PUBLISHED;
		this.publishedAt = publishedAt;
	}

	public void archive() {
		this.status = TrainingVersionStatus.ARCHIVED;
	}
}
