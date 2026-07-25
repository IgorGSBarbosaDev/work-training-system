package dev.igorbarbosa.worktrainingsystem.shared.storage.minio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.storage")
public record ObjectStorageProperties(
		@NotNull URI endpoint,
		@NotNull URI publicEndpoint,
		@NotBlank String accessKey,
		@NotBlank String secretKey,
		@NotBlank String bucket,
		@NotBlank String region,
		@NotNull Duration uploadUrlTtl,
		@NotNull Duration downloadUrlTtl,
		@NotNull Duration playbackUrlTtl) {
}
