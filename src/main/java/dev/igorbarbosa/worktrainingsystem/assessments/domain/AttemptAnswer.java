package dev.igorbarbosa.worktrainingsystem.assessments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "attempt_id", nullable = false, updatable = false) private UUID attemptId;
	@Column(name = "questionnaire_id", nullable = false, updatable = false) private UUID questionnaireId;
	@Column(name = "question_id", nullable = false, updatable = false) private UUID questionId;
	@Column(name = "selected_option_id", nullable = false, updatable = false) private UUID selectedOptionId;
	@Column(name = "question_statement_snapshot", nullable = false, updatable = false, length = 2000) private String questionStatementSnapshot;
	@Column(name = "selected_option_text_snapshot", nullable = false, updatable = false, length = 1000) private String selectedOptionTextSnapshot;
	@Column(nullable = false, updatable = false) private boolean correct;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected AttemptAnswer() {}
	public AttemptAnswer(UUID organizationId, UUID attemptId, UUID questionnaireId, UUID questionId,
			UUID selectedOptionId, String questionStatement, String selectedOptionText, boolean correct, Instant now) {
		this.organizationId = organizationId; this.attemptId = attemptId; this.questionnaireId = questionnaireId;
		this.questionId = questionId; this.selectedOptionId = selectedOptionId;
		this.questionStatementSnapshot = questionStatement; this.selectedOptionTextSnapshot = selectedOptionText;
		this.correct = correct; this.createdAt = now;
	}
	public UUID getQuestionId() { return questionId; }
	public UUID getSelectedOptionId() { return selectedOptionId; }
	public String getQuestionStatementSnapshot() { return questionStatementSnapshot; }
	public String getSelectedOptionTextSnapshot() { return selectedOptionTextSnapshot; }
	public boolean isCorrect() { return correct; }
}
