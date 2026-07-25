package dev.igorbarbosa.worktrainingsystem.assessments.api;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentAttemptDetailResponse(UUID id, UUID assignmentId, UUID employeeId, UUID trainingId,
		UUID trainingVersionId, UUID questionnaireId, int attemptNumber, Instant submittedAt,
		BigDecimal score, BigDecimal passingScore, AssessmentResult result, List<Answer> answers) {
	public record Answer(UUID questionId, String question, UUID selectedOptionId, String selectedOption, boolean correct) {}
}
