package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Question;
import java.util.UUID;

public record QuestionResponse(UUID id, UUID questionnaireId, String statement, int order, RegistrationStatus status) {

	public static QuestionResponse from(Question question) {
		return new QuestionResponse(question.getId(), question.getQuestionnaireId(), question.getStatement(),
				question.getDisplayOrder(), question.getStatus());
	}
}
