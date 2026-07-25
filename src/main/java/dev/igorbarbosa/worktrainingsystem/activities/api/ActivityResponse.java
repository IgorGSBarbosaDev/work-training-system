package dev.igorbarbosa.worktrainingsystem.activities.api;

import dev.igorbarbosa.worktrainingsystem.activities.domain.Activity;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(UUID id, String name, String description, RegistrationStatus status,
		Instant createdAt, Instant updatedAt) {
	public static ActivityResponse from(Activity activity) {
		return new ActivityResponse(activity.getId(), activity.getName(), activity.getDescription(), activity.getStatus(),
				activity.getCreatedAt(), activity.getUpdatedAt());
	}
}
