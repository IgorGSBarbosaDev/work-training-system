package dev.igorbarbosa.worktrainingsystem.progress.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class VideoPlaybackServiceTest {
	@Mock TrainingExecutionCatalog trainings;
	@Mock AssignmentExecutionPort assignments;
	@Mock VideoProgressService progress;
	@Mock ObjectStorage storage;
	@Mock CurrentUserProvider currentUser;
	@Test
	void deniesEmployeeWithoutAssignmentForExactVersion() {
		UUID videoId = UUID.randomUUID(); UUID versionId = UUID.randomUUID(); UUID employeeId = UUID.randomUUID();
		when(trainings.requireVideo(videoId)).thenReturn(new TrainingExecutionCatalog.ExecutionVideo(videoId,
				UUID.randomUUID(), versionId, "Video", null, 1, 60, true, UUID.randomUUID(), "secret/key.mp4"));
		when(currentUser.requireCurrentUser()).thenReturn(new CurrentUser(UUID.randomUUID(), UUID.randomUUID(),
				UserRole.EMPLOYEE, employeeId, Set.of()));
		when(assignments.findPlaybackAssignment(employeeId, versionId)).thenReturn(Optional.empty());
		VideoPlaybackService service = new VideoPlaybackService(trainings, assignments, progress, storage, currentUser);
		assertThatThrownBy(() -> service.playbackUrl(videoId)).isInstanceOf(AccessDeniedException.class);
	}
}
