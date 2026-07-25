package dev.igorbarbosa.worktrainingsystem.assessments.api;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentAttemptResponse(UUID attemptId, int attemptNumber, BigDecimal score,
		BigDecimal passingScore, AssessmentResult result, AssignmentStatus assignmentStatus,
		Instant completedAt, Instant nextAttemptAt) {}
