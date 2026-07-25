package dev.igorbarbosa.worktrainingsystem.assessments.api;

import java.util.List;
import java.util.UUID;

/** Deliberately contains no correctness property, including in the generated OpenAPI schema. */
public record QuestionnaireDeliveryResponse(UUID id, String title, boolean shuffleQuestions, List<Question> questions) {
	public record Question(UUID id, String statement, List<Option> options) {}
	public record Option(UUID id, String text) {}
}
