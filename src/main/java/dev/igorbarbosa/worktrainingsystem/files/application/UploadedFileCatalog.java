package dev.igorbarbosa.worktrainingsystem.files.application;

import java.util.UUID;

/** Public boundary for domain modules that attach completed private files. */
public interface UploadedFileCatalog {
	FileReference requireTrainingVideo(UUID fileId);
	FileReference requireTrainingVideo(String objectKey);
	FileReference requireExternalCertificate(UUID fileId, UUID employeeId);
	record FileReference(UUID id, String objectKey, String contentType, long sizeBytes) {}
}
