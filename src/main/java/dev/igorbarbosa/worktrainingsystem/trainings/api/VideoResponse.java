package dev.igorbarbosa.worktrainingsystem.trainings.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Video;
import java.util.UUID;

public record VideoResponse(UUID id, UUID moduleId, String title, String description, int order, int durationSeconds,
		String storageObjectKey, boolean required, RegistrationStatus status, UUID fileId) {

	public static VideoResponse from(Video video) {
		return new VideoResponse(video.getId(), video.getModuleId(), video.getTitle(), video.getDescription(),
				video.getDisplayOrder(), video.getDurationSeconds(), video.getStorageObjectKey(), video.isRequired(),
				video.getStatus(), video.getFileId());
	}
}
