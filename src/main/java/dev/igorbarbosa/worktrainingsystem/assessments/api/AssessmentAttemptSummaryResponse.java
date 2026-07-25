package dev.igorbarbosa.worktrainingsystem.assessments.api;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentAttemptSummaryResponse(UUID id, UUID assignmentId, UUID questionnaireId,
		int attemptNumber, Instant submittedAt, BigDecimal score, BigDecimal passingScore, AssessmentResult result) {}
