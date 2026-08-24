package dev.igorbarbosa.worktrainingsystem;

import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectMetadata;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.PresignedObjectUrl;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17.11-alpine3.24"));
	}

	@Bean
	@Primary
	ObjectStorage testObjectStorage() {
		return new InMemoryObjectStorage();
	}

	private static final class InMemoryObjectStorage implements ObjectStorage {
		private final Map<String, Stored> objects = new ConcurrentHashMap<>();

		@Override
		public void upload(String objectKey, InputStream content, long contentLength, String contentType) {
			try (content; var output = new ByteArrayOutputStream()) {
				content.transferTo(output);
				objects.put(objectKey, new Stored(output.toByteArray(), contentType, Instant.now()));
			} catch (IOException exception) {
				throw new IllegalStateException("Could not upload test object", exception);
			}
		}

		@Override
		public StoredObject download(String objectKey) {
			Stored value = require(objectKey);
			return new StoredObject(new ByteArrayInputStream(value.content()), value.content().length, value.contentType());
		}

		@Override
		public ObjectMetadata head(String objectKey) {
			Stored value = require(objectKey);
			return new ObjectMetadata(value.content().length, value.contentType(), null, "test-etag", value.createdAt());
		}

		@Override
		public void delete(String objectKey) { objects.remove(objectKey); }

		@Override
		public PresignedObjectUrl presignUpload(String objectKey) {
			return url(objectKey, "upload");
		}

		@Override
		public PresignedObjectUrl presignDownload(String objectKey) {
			return url(objectKey, "download");
		}

		@Override
		public PresignedObjectUrl presignPlayback(String objectKey) {
			return url(objectKey, "playback");
		}

		private PresignedObjectUrl url(String objectKey, String operation) {
			return new PresignedObjectUrl(URI.create("http://test-storage/" + operation + "/" + objectKey),
					Instant.now().plusSeconds(900));
		}

		private Stored require(String objectKey) {
			Stored value = objects.get(objectKey);
			if (value == null) throw new IllegalStateException("Test object does not exist: " + objectKey);
			return value;
		}

		private record Stored(byte[] content, String contentType, Instant createdAt) {}
	}
}
