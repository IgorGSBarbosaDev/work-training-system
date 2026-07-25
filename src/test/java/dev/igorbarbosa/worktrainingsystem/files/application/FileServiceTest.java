package dev.igorbarbosa.worktrainingsystem.files.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.files.api.UploadRequest;
import dev.igorbarbosa.worktrainingsystem.files.domain.FilePurpose;
import dev.igorbarbosa.worktrainingsystem.files.domain.FileState;
import dev.igorbarbosa.worktrainingsystem.files.domain.UploadedFile;
import dev.igorbarbosa.worktrainingsystem.files.persistence.UploadedFileRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectMetadata;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.PresignedObjectUrl;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
	@Mock UploadedFileRepository files;
	@Mock ObjectStorage storage;
	@Mock CurrentUserProvider currentUser;
	@Mock AuthorizationService authorization;
	private FileService service;

	@BeforeEach
	void setUp() {
		service = new FileService(files, storage, currentUser, authorization, Clock.fixed(NOW, ZoneOffset.UTC));
		org.mockito.Mockito.lenient().when(files.saveAndFlush(any(UploadedFile.class))).thenAnswer(invocation -> {
			UploadedFile file = invocation.getArgument(0); ReflectionTestUtils.setField(file, "id", UUID.randomUUID()); return file;
		});
	}

	@Test
	void employeeCanRequestOnlyOwnAllowedPhotoAndNeverReceivesRawKey() {
		UUID employeeId = UUID.randomUUID();
		when(currentUser.requireCurrentUser()).thenReturn(user(UserRole.EMPLOYEE, employeeId));
		when(storage.presignUpload(any())).thenReturn(new PresignedObjectUrl(URI.create("https://storage/upload"), NOW.plusSeconds(900)));
		var response = service.request(new UploadRequest(FilePurpose.EMPLOYEE_PHOTO, "photo.webp", "image/webp",
				1024, null, null));
		assertThat(response.objectKey()).isNull();
		assertThat(response.state()).isEqualTo(FileState.REQUESTED);
		assertThatThrownBy(() -> service.request(new UploadRequest(FilePurpose.TRAINING_VIDEO, "video.mp4", "video/mp4",
				1024, null, null))).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsDisallowedTypeAndOversizedFileBeforePresigning() {
		when(currentUser.requireCurrentUser()).thenReturn(user(UserRole.ADMIN, null));
		assertThatThrownBy(() -> service.request(new UploadRequest(FilePurpose.TRAINING_VIDEO, "video.mov", "video/quicktime",
				1024, null, null))).isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("UPLOAD_TYPE_INVALID");
		assertThatThrownBy(() -> service.request(new UploadRequest(FilePurpose.EMPLOYEE_PHOTO, "photo.png", "image/png",
				6L * 1024 * 1024, null, UUID.randomUUID()))).isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("UPLOAD_TOO_LARGE");
	}

	@Test
	void completionVerifiesActualObjectMetadata() {
		CurrentUser admin = user(UserRole.ADMIN, null); when(currentUser.requireCurrentUser()).thenReturn(admin);
		UploadedFile file = pending(admin.userId(), FilePurpose.TRAINING_VIDEO, "video/mp4", 100);
		when(files.findByIdAndOrganizationId(file.getId(), DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(file));
		when(storage.head(file.getObjectKey())).thenReturn(new ObjectMetadata(99, "video/mp4", null, "etag", NOW));
		assertThatThrownBy(() -> service.complete(file.getId())).isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("UPLOAD_METADATA_MISMATCH");
		assertThat(file.getState()).isEqualTo(FileState.FAILED);
		verify(storage).delete(file.getObjectKey());
	}

	@Test
	void completedTrainingVideoBecomesAttachableOnlyAfterVerifiedHead() {
		CurrentUser admin = user(UserRole.ADMIN, null); when(currentUser.requireCurrentUser()).thenReturn(admin);
		UploadedFile file = pending(admin.userId(), FilePurpose.TRAINING_VIDEO, "video/mp4", 100);
		when(files.findByIdAndOrganizationId(file.getId(), DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(file));
		when(storage.head(file.getObjectKey())).thenReturn(new ObjectMetadata(100, "video/mp4", null, "etag", NOW));
		service.complete(file.getId());
		assertThat(service.requireTrainingVideo(file.getId()).objectKey()).isEqualTo(file.getObjectKey());
	}

	private UploadedFile pending(UUID userId, FilePurpose purpose, String type, long size) {
		UploadedFile file = new UploadedFile(DEFAULT_ORGANIZATION_ID, purpose, userId, null, "file", "private/" + UUID.randomUUID(),
				type, size, null, NOW.plusSeconds(900));
		ReflectionTestUtils.setField(file, "id", UUID.randomUUID()); return file;
	}
	private CurrentUser user(UserRole role, UUID employeeId) {
		return new CurrentUser(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID, role, employeeId, Set.of());
	}
}
