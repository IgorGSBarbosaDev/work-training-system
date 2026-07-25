package dev.igorbarbosa.worktrainingsystem.activities.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JobActivityRequest(@NotNull UUID activityId, Boolean applyToCurrentEmployees) {
	public boolean shouldApplyToCurrentEmployees() { return applyToCurrentEmployees == null || applyToCurrentEmployees; }
}
