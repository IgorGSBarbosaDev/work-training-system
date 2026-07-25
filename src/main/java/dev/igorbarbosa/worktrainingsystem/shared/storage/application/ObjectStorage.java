package dev.igorbarbosa.worktrainingsystem.shared.storage.application;

import java.io.InputStream;

public interface ObjectStorage {

	void upload(String objectKey, InputStream content, long contentLength, String contentType);

	StoredObject download(String objectKey);

	ObjectMetadata head(String objectKey);

	void delete(String objectKey);

	PresignedObjectUrl presignUpload(String objectKey);

	PresignedObjectUrl presignDownload(String objectKey);

	PresignedObjectUrl presignPlayback(String objectKey);
}
