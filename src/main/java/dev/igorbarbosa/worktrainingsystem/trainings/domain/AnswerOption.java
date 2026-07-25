package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "answer_options")
public class AnswerOption extends BaseEntity {

	@Column(name = "question_id", nullable = false, updatable = false)
	private UUID questionId;

	@Column(nullable = false, length = 1000)
	private String text;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected AnswerOption() {
	}

	public AnswerOption(UUID questionId, String text, boolean correct, int displayOrder,
			RegistrationStatus status) {
		this.questionId = questionId;
		this.text = text;
		this.correct = correct;
		this.displayOrder = displayOrder;
		this.status = status;
	}

	public UUID getQuestionId() {
		return questionId;
	}

	public String getText() {
		return text;
	}

	public boolean isCorrect() {
		return correct;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String text, boolean correct, int displayOrder) {
		this.text = text;
		this.correct = correct;
		this.displayOrder = displayOrder;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
	public void changeOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
