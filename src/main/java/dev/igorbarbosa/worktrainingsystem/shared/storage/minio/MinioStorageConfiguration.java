package dev.igorbarbosa.worktrainingsystem.shared.storage.minio;

import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MinioStorageConfiguration {

	@Bean
	MinioClient minioClient(ObjectStorageProperties properties) {
		return client(properties.endpoint().toString(), properties);
	}

	@Bean("presigningMinioClient")
	MinioClient presigningMinioClient(ObjectStorageProperties properties) {
		return client(properties.publicEndpoint().toString(), properties);
	}

	@Bean
	ObjectStorage objectStorage(
			@Qualifier("minioClient") MinioClient minioClient,
			@Qualifier("presigningMinioClient") MinioClient presigningMinioClient,
			ObjectStorageProperties properties) {
		return new MinioObjectStorage(minioClient, presigningMinioClient, properties);
	}

	private MinioClient client(String endpoint, ObjectStorageProperties properties) {
		return MinioClient.builder()
				.endpoint(endpoint)
				.credentials(properties.accessKey(), properties.secretKey())
				.region(properties.region())
				.build();
	}
}
