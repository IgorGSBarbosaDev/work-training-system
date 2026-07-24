package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.AnswerOption;
import java.util.UUID;

public record AnswerOptionResponse(UUID id, UUID questionId, String text, boolean correct, int order,
		RegistrationStatus status) {

	public static AnswerOptionResponse from(AnswerOption option) {
		return new AnswerOptionResponse(option.getId(), option.getQuestionId(), option.getText(), option.isCorrect(),
				option.getDisplayOrder(), option.getStatus());
	}
}
