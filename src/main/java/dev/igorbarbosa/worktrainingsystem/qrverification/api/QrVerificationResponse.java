package dev.igorbarbosa.worktrainingsystem.qrverification.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QrVerificationResponse(Employee employee, List<Training> trainings, List<Training> regulatoryStandards, List<Activity> activities) {
	public record Employee(String name, String registration, String job) {}
	public record Training(String name, String code, boolean isRegulatoryStandard, Instant completedAt, LocalDate expiresAt, String status) {}
	public record Activity(String name, String status, List<String> pendingTrainings) {}
}
