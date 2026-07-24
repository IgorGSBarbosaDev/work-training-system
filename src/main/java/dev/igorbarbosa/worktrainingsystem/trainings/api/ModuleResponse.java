package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingModule;
import java.util.UUID;

public record ModuleResponse(UUID id, UUID trainingVersionId, String title, String description, int order,
		RegistrationStatus status) {

	public static ModuleResponse from(TrainingModule module) {
		return new ModuleResponse(module.getId(), module.getTrainingVersionId(), module.getTitle(), module.getDescription(),
				module.getDisplayOrder(), module.getStatus());
	}
}
