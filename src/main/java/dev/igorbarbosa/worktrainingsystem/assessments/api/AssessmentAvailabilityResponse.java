package dev.igorbarbosa.worktrainingsystem.assessments.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentAvailabilityResponse(UUID assignmentId, UUID questionnaireId, BigDecimal passingScore,
		int attemptsUsed, Integer maxAttempts, Integer attemptsRemaining, boolean available, Instant nextAttemptAt) {}
