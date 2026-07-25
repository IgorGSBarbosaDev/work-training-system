package dev.igorbarbosa.worktrainingsystem.progress.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoProgressResponse(UUID assignmentId, UUID videoId, long positionSeconds,
		BigDecimal watchedSeconds, BigDecimal percentageWatched, boolean completed,
		AssignmentStatus assignmentStatus, Instant updatedAt) {
}
