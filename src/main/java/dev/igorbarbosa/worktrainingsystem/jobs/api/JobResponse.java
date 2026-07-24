package dev.igorbarbosa.worktrainingsystem.jobs.api;

import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
		UUID id,
		String name,
		String description,
		RegistrationStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static JobResponse from(Job job) {
		return new JobResponse(
				job.getId(),
				job.getName(),
				job.getDescription(),
				job.getStatus(),
				job.getCreatedAt(),
				job.getUpdatedAt());
	}
}
