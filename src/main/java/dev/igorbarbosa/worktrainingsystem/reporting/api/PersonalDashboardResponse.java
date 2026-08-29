package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PersonalDashboardResponse(ContinueTraining continueTraining, Counts counts,
		List<TrainingSummary> pendingTrainings, List<TrainingSummary> expiringTrainings,
		List<ActivitySummary> blockedActivities) {
	public record ContinueTraining(UUID assignmentId, String trainingName, BigDecimal progressPercentage,
			ResumeAt resumeAt) {}
	public record ResumeAt(UUID videoId, long positionSeconds) {}
	public record Counts(long pending, long inProgress, long expiringSoon, long expired, long completed,
			long availableActivities, long blockedActivities) {}
	public record TrainingSummary(UUID assignmentId, UUID trainingId, String trainingName, String status,
			LocalDate dueDate, BigDecimal progressPercentage) {}
	public record ActivitySummary(UUID activityId, String activityName, String status,
			List<String> blockingTrainings) {}
}
