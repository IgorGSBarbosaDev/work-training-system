package dev.igorbarbosa.worktrainingsystem.progress.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record VideoProgressRequest(@PositiveOrZero long positionSeconds,
		@NotNull @DecimalMin("0.000") BigDecimal watchedSeconds,
		@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @JsonAlias("percentageWatched") BigDecimal reportedPercentage,
		@NotNull Instant eventAt,
		@Size(max = 200) String eventId,
		boolean finalEvent) {
}
