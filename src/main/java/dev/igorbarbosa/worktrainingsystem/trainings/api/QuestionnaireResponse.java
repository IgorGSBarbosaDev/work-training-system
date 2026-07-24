package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Questionnaire;
import java.math.BigDecimal;
import java.util.UUID;

public record QuestionnaireResponse(UUID id, UUID moduleId, String title, BigDecimal passingScore,
		Integer maxAttempts, int retryIntervalMinutes, boolean shuffleQuestions, RegistrationStatus status) {

	public static QuestionnaireResponse from(Questionnaire questionnaire) {
		return new QuestionnaireResponse(questionnaire.getId(), questionnaire.getModuleId(), questionnaire.getTitle(),
				questionnaire.getPassingScore(), questionnaire.getMaxAttempts(), questionnaire.getRetryIntervalMinutes(),
				questionnaire.isShuffleQuestions(), questionnaire.getStatus());
	}
}
