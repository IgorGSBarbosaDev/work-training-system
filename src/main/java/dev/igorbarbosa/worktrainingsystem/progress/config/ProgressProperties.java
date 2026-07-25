package dev.igorbarbosa.worktrainingsystem.progress.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.progress")
public record ProgressProperties(@NotNull Duration futureTolerance, @NotNull Duration maxEventAge,
		@NotNull Duration minimumEventInterval,
		@DecimalMin("0.0") BigDecimal watchedToleranceSeconds,
		@Positive BigDecimal maximumPlaybackRate,
		@DecimalMin("0.0") BigDecimal reportedPercentageTolerance) {
}
