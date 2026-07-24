package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "questionnaires")
public class Questionnaire extends BaseEntity {

	@Column(name = "module_id", nullable = false, unique = true, updatable = false)
	private UUID moduleId;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
	private BigDecimal passingScore;

	@Column(name = "max_attempts")
	private Integer maxAttempts;

	@Column(name = "retry_interval_minutes", nullable = false)
	private int retryIntervalMinutes;

	@Column(name = "shuffle_questions", nullable = false)
	private boolean shuffleQuestions;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Questionnaire() {
	}

	public Questionnaire(UUID moduleId, String title, BigDecimal passingScore, Integer maxAttempts,
			int retryIntervalMinutes, boolean shuffleQuestions, RegistrationStatus status) {
		this.moduleId = moduleId;
		this.title = title;
		this.passingScore = passingScore;
		this.maxAttempts = maxAttempts;
		this.retryIntervalMinutes = retryIntervalMinutes;
		this.shuffleQuestions = shuffleQuestions;
		this.status = status;
	}

	public UUID getModuleId() {
		return moduleId;
	}

	public String getTitle() {
		return title;
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

	public boolean isShuffleQuestions() {
		return shuffleQuestions;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String title, BigDecimal passingScore, Integer maxAttempts,
			int retryIntervalMinutes, boolean shuffleQuestions) {
		this.title = title;
		this.passingScore = passingScore;
		this.maxAttempts = maxAttempts;
		this.retryIntervalMinutes = retryIntervalMinutes;
		this.shuffleQuestions = shuffleQuestions;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
}
