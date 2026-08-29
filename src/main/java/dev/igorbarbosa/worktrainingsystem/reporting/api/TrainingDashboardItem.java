package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.math.BigDecimal;
import java.util.UUID;

public record TrainingDashboardItem(UUID trainingId, String trainingName, String trainingCode, long assigned,
		long notStarted, long inProgress, long latestAssessmentApproved, long latestAssessmentFailed, long completed,
		long expired, BigDecimal completionRate, BigDecimal averageLatestAssessment, BigDecimal averageCompletionHours) {}
