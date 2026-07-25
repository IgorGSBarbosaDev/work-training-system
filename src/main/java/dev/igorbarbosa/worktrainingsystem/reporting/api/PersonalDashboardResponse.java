package dev.igorbarbosa.worktrainingsystem.reporting.api;

public record PersonalDashboardResponse(Counts counts) {
	public record Counts(long pending, long inProgress, long expiringSoon, long expired, long completed) {}
}
