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
@Table(name = "questions")
public class Question extends BaseEntity {

	@Column(name = "questionnaire_id", nullable = false, updatable = false)
	private UUID questionnaireId;

	@Column(nullable = false, length = 2000)
	private String statement;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Question() {
	}

	public Question(UUID questionnaireId, String statement, int displayOrder, RegistrationStatus status) {
		this.questionnaireId = questionnaireId;
		this.statement = statement;
		this.displayOrder = displayOrder;
		this.status = status;
	}

	public UUID getQuestionnaireId() {
		return questionnaireId;
	}

	public String getStatement() {
		return statement;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String statement, int displayOrder) {
		this.statement = statement;
		this.displayOrder = displayOrder;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
	public void changeOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
