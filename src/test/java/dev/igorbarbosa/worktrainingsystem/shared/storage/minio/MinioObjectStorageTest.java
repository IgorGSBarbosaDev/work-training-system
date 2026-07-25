package dev.igorbarbosa.worktrainingsystem.shared.storage.minio;

import static org.assertj.core.api.Assertions.assertThat;

import dev.igorbarbosa.worktrainingsystem.shared.storage.application.PresignedObjectUrl;
import io.minio.MinioClient;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MinioObjectStorageTest {

	@Test
	void presignsAgainstPublicEndpointWithoutConnectingToMinio() {
		ObjectStorageProperties properties = new ObjectStorageProperties(
				URI.create("http://minio:9000"),
				URI.create("http://localhost:9000"),
				"minioadmin",
				"minioadmin123",
				"private-files",
				"us-east-1",
				Duration.ofMinutes(10),
				Duration.ofMinutes(20),
				Duration.ofMinutes(30));
		MinioClient internalClient = client(properties.endpoint(), properties);
		MinioClient publicClient = client(properties.publicEndpoint(), properties);
		MinioObjectStorage storage = new MinioObjectStorage(internalClient, publicClient, properties);
		Instant beforePresigning = Instant.now();

		PresignedObjectUrl url = storage.presignPlayback("videos/training.mp4");

		assertThat(url.url().getHost()).isEqualTo("localhost");
		assertThat(url.url().getRawQuery()).contains("X-Amz-Algorithm", "X-Amz-Signature");
		assertThat(url.expiresAt()).isBetween(
				beforePresigning.plus(Duration.ofMinutes(30)),
				Instant.now().plus(Duration.ofMinutes(30)));
	}

	private MinioClient client(URI endpoint, ObjectStorageProperties properties) {
		return MinioClient.builder()
				.endpoint(endpoint.toString())
				.credentials(properties.accessKey(), properties.secretKey())
				.region(properties.region())
				.build();
	}
}
