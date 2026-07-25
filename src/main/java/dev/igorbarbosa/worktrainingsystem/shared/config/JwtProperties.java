package dev.igorbarbosa.worktrainingsystem.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotBlank String signingSecret,
		@NotNull Duration accessTokenTtl,
		@NotNull Duration refreshTokenTtl) {

	public byte[] signingKey() {
		byte[] key = signingSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		if (key.length < 32) {
			throw new IllegalStateException("app.jwt.signing-secret must contain at least 32 UTF-8 bytes");
		}
		return key;
	}
}
