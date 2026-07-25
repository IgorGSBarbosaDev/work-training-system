package dev.igorbarbosa.worktrainingsystem.employees.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EmployeePhotoServiceTest {
	@Mock EmployeeService employees;
	@Mock ObjectStorage storage;

	@Test
	void uploadsWithGeneratedKeyAndPersistsOnlyAfterStorageSucceeds() {
		UUID employeeId = UUID.randomUUID();
		byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
		MockMultipartFile file = new MockMultipartFile("file", "unsafe/../../photo.png", "image/png", png);
		EmployeeResponse response = org.mockito.Mockito.mock(EmployeeResponse.class);
		when(employees.replacePhoto(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(response);

		new EmployeePhotoService(employees, storage).upload(employeeId, file);

		verify(storage).upload(startsWith("organizations/00000000-0000-0000-0000-000000000001/employees/"
				+ employeeId + "/photos/"), any(InputStream.class), org.mockito.ArgumentMatchers.eq((long) png.length),
				org.mockito.ArgumentMatchers.eq("image/png"));
		verify(employees).replacePhoto(org.mockito.ArgumentMatchers.eq(employeeId), any(),
				org.mockito.ArgumentMatchers.eq("image/png"), org.mockito.ArgumentMatchers.eq((long) png.length));
	}

	@Test
	void rejectsSpoofedImageContentBeforeUpload() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "not an image".getBytes());

		assertThatThrownBy(() -> new EmployeePhotoService(employees, storage).upload(UUID.randomUUID(), file))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("PHOTO_CONTENT_INVALID");
		verify(storage, never()).upload(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
	}
}
