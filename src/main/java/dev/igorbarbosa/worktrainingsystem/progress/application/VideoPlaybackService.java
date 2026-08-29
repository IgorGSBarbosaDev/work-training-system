package dev.igorbarbosa.worktrainingsystem.progress.application;

import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.progress.api.PlaybackUrlResponse;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoPlaybackService {
	private final TrainingExecutionCatalog trainings;
	private final AssignmentExecutionPort assignments;
	private final VideoProgressService progress;
	private final ObjectStorage storage;
	private final CurrentUserProvider currentUser;
	public VideoPlaybackService(TrainingExecutionCatalog trainings, AssignmentExecutionPort assignments,
			VideoProgressService progress, ObjectStorage storage, CurrentUserProvider currentUser) {
		this.trainings = trainings; this.assignments = assignments; this.progress = progress;
		this.storage = storage; this.currentUser = currentUser;
	}

	@Transactional
	public PlaybackUrlResponse playbackUrl(UUID videoId) {
		var video = trainings.requireVideo(videoId);
		CurrentUser actor = currentUser.requireCurrentUser();
		long resume = 0;
		if (actor.role() == UserRole.EMPLOYEE && actor.employeeId() != null) {
			var assignment = assignments.findPlaybackAssignment(actor.employeeId(), video.trainingVersionId())
					.orElseThrow(() -> new AccessDeniedException("O vídeo não pertence a uma atribuição ativa do colaborador."));
			resume = progress.resumePosition(assignment.id(), videoId);
		} else if (actor.role() != UserRole.ADMIN) {
			throw new AccessDeniedException("A reprodução é restrita ao administrador ou colaborador atribuído.");
		}
		var url = storage.presignPlayback(video.objectKey());
		return new PlaybackUrlResponse(url.url(), url.expiresAt(), resume);
	}
}
