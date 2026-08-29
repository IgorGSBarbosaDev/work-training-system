package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.util.List;
import java.util.UUID;

public record ActivityDashboardItem(UUID activityId, String activityName, long relatedJobs, long requirements,
		long availableEmployees, long expiringEmployees, long blockedEmployees, List<String> mainBlockingTrainings) {}
