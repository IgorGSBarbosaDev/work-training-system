package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.time.Instant;

public record DashboardOverviewResponse(long activeEmployees, long registeredTrainings, long assignedTrainings,
		long notStarted, long inProgress, long completed, long failed, long expired, long expiringIn30Days,
		Instant generatedAt) {}
