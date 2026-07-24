package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Training;
import java.util.UUID;

public record TrainingResponse(
		UUID id,
		String name,
		String code,
		String description,
		String category,
		boolean regulatoryStandard,
		RegistrationStatus status) {

	public static TrainingResponse from(Training training) {
		return new TrainingResponse(training.getId(), training.getName(), training.getCode(), training.getDescription(),
				training.getCategory(), training.isRegulatoryStandard(), training.getStatus());
	}
}
