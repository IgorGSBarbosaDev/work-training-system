package dev.igorbarbosa.worktrainingsystem.reporting.api;

import java.time.LocalDate;
import java.util.UUID;

public record DashboardFilter(UUID unitId, UUID sectorId, UUID jobId, UUID activityId, UUID trainingId,
		String status, LocalDate periodFrom, LocalDate periodTo) {
	public DashboardFilter {
		status = status == null || status.isBlank() ? null : status.trim().toUpperCase();
		if (periodFrom != null && periodTo != null && periodFrom.isAfter(periodTo)) {
			throw new InvalidDashboardFilterException("A data inicial do período não pode ser posterior à data final.");
		}
	}
}
