package dev.igorbarbosa.worktrainingsystem.activities.api;

import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequirementRequest(@NotNull UUID trainingId, @NotNull RequirementVersionPolicy versionPolicy,
		UUID trainingVersionId, Boolean required, Boolean applyToCurrentEmployees) {
	public boolean shouldApplyToCurrentEmployees() { return applyToCurrentEmployees == null || applyToCurrentEmployees; }
}
