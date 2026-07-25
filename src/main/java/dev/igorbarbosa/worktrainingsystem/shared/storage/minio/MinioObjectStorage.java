package dev.igorbarbosa.worktrainingsystem.shared.storage.minio;

import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorageException;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectMetadata;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.PresignedObjectUrl;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.StoredObject;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

final class MinioObjectStorage implements ObjectStorage {

	private static final Duration MAX_PRESIGNED_URL_VALIDITY = Duration.ofDays(7);

	private final MinioClient minioClient;
	private final MinioClient presigningMinioClient;
	private final String bucket;
	private final Duration uploadUrlTtl;
	private final Duration downloadUrlTtl;
	private final Duration playbackUrlTtl;

	MinioObjectStorage(
			MinioClient minioClient,
			MinioClient presigningMinioClient,
			ObjectStorageProperties properties) {
		this.minioClient = minioClient;
		this.presigningMinioClient = presigningMinioClient;
		this.bucket = properties.bucket();
		this.uploadUrlTtl = properties.uploadUrlTtl();
		this.downloadUrlTtl = properties.downloadUrlTtl();
		this.playbackUrlTtl = properties.playbackUrlTtl();
	}

	@Override
	public void upload(String objectKey, InputStream content, long contentLength, String contentType) {
		validateObjectKey(objectKey);
		if (content == null || contentLength < 0 || contentType == null || contentType.isBlank()) {
			throw new IllegalArgumentException("Content, content type and a non-negative content length are required");
		}
		try {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(objectKey)
					.stream(content, contentLength, -1)
					.contentType(contentType)
					.build());
		}
		catch (Exception exception) {
			throw storageFailure("upload", exception);
		}
	}

	@Override
	public StoredObject download(String objectKey) {
		validateObjectKey(objectKey);
		try {
			GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
					.bucket(bucket)
					.object(objectKey)
					.build());
			return new StoredObject(
					response,
					contentLength(response.headers().get("Content-Length")),
					response.headers().get("Content-Type"));
		}
		catch (Exception exception) {
			throw storageFailure("download", exception);
		}
	}

	@Override
	public void delete(String objectKey) {
		validateObjectKey(objectKey);
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(bucket)
					.object(objectKey)
					.build());
		}
		catch (Exception exception) {
			throw storageFailure("delete", exception);
		}
	}

	@Override
	public ObjectMetadata head(String objectKey) {
		validateObjectKey(objectKey);
		try {
			StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
					.bucket(bucket).object(objectKey).build());
			String checksum = response.userMetadata().get("sha256");
			return new ObjectMetadata(response.size(), response.contentType(), checksum,
					response.etag(), response.lastModified() == null ? null : response.lastModified().toInstant());
		}
		catch (Exception exception) {
			throw storageFailure("read metadata", exception);
		}
	}

	@Override
	public PresignedObjectUrl presignUpload(String objectKey) {
		return presign(objectKey, uploadUrlTtl, Method.PUT, "presigned upload");
	}

	@Override
	public PresignedObjectUrl presignDownload(String objectKey) {
		return presign(objectKey, downloadUrlTtl, Method.GET, "presigned download");
	}

	@Override
	public PresignedObjectUrl presignPlayback(String objectKey) {
		return presign(objectKey, playbackUrlTtl, Method.GET, "presigned playback");
	}

	private PresignedObjectUrl presign(String objectKey, Duration validity, Method method, String operation) {
		validateObjectKey(objectKey);
		validateValidity(validity);
		try {
			String url = presigningMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
					.method(method)
					.bucket(bucket)
					.object(objectKey)
					.expiry((int) validity.toSeconds(), TimeUnit.SECONDS)
					.build());
			return new PresignedObjectUrl(URI.create(url), Instant.now().plus(validity));
		}
		catch (Exception exception) {
			throw storageFailure(operation, exception);
		}
	}

	private void validateObjectKey(String objectKey) {
		if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/") || objectKey.length() > 1024
				|| objectKey.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("A valid relative object key is required");
		}
	}

	private void validateValidity(Duration validity) {
		if (validity == null || validity.isZero() || validity.isNegative()
				|| validity.compareTo(MAX_PRESIGNED_URL_VALIDITY) > 0) {
			throw new IllegalArgumentException("Presigned URL validity must be between one second and seven days");
		}
	}

	private long contentLength(String contentLength) {
		return contentLength == null ? -1 : Long.parseLong(contentLength);
	}

	private ObjectStorageException storageFailure(String operation, Exception exception) {
		if (exception instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		}
		return new ObjectStorageException(operation, exception);
	}
}
