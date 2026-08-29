package dev.igorbarbosa.worktrainingsystem.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.igorbarbosa.worktrainingsystem.qrverification.application.QrVerificationProperties;
import dev.igorbarbosa.worktrainingsystem.shared.storage.minio.ObjectStorageProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class FoundationPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class)
			.withPropertyValues(
					"app.jwt.issuer=test-issuer",
					"app.jwt.audience=test-api",
					"app.jwt.signing-secret=a-test-signing-secret-that-is-long-enough",
					"app.jwt.access-token-ttl=15m",
					"app.jwt.refresh-token-ttl=7d",
					"app.storage.endpoint=http://minio:9000",
					"app.storage.public-endpoint=http://localhost:9000",
					"app.storage.access-key=minioadmin",
					"app.storage.secret-key=minioadmin123",
					"app.storage.bucket=private-files",
					"app.storage.region=us-east-1",
					"app.storage.upload-url-ttl=10m",
					"app.storage.download-url-ttl=20m",
					"app.storage.playback-url-ttl=30m",
					"app.qr.public-base-url=http://localhost:3000/");

	@Test
	void bindsJwtAndStorageConfiguration() {
		contextRunner.run(context -> {
			JwtProperties jwt = context.getBean(JwtProperties.class);
			ObjectStorageProperties storage = context.getBean(ObjectStorageProperties.class);

			assertThat(jwt.issuer()).isEqualTo("test-issuer");
			assertThat(jwt.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
			assertThat(storage.endpoint().getHost()).isEqualTo("minio");
			assertThat(storage.publicEndpoint().getHost()).isEqualTo("localhost");
			assertThat(storage.playbackUrlTtl()).isEqualTo(Duration.ofMinutes(30));
			assertThat(context.getBean(QrVerificationProperties.class).verificationUrl("token").toString())
					.isEqualTo("http://localhost:3000/verificar/token");
		});
	}

	@Configuration(proxyBeanMethods = false)
		@EnableConfigurationProperties({JwtProperties.class, ObjectStorageProperties.class, QrVerificationProperties.class})
	static class PropertiesConfiguration {
	}
}
