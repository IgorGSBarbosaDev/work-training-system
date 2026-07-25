package dev.igorbarbosa.worktrainingsystem.identity.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.identity")
public record IdentityProperties(
		@Min(1) int loginFailureThreshold,
		@NotNull Duration loginLockDuration,
		@NotNull Duration loginAttemptWindow,
		@NotNull Duration passwordResetTokenTtl,
		@NotNull URI passwordResetUrl,
		@Min(12) int bcryptStrength) {
}
