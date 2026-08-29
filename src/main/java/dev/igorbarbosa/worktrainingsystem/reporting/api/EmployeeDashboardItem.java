package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeDashboardItem(UUID employeeId, String employeeName, String registration, UUID unitId,
		String unitName, UUID sectorId, String sectorName, UUID jobId, String jobName, long mandatoryTrainings,
		long optionalTrainings, BigDecimal averageProgress, BigDecimal averageLatestAssessment, long completions,
		long expirations, long availableActivities, long blockedActivities) {}
