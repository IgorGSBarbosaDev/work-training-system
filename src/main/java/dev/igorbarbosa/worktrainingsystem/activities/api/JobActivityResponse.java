package dev.igorbarbosa.worktrainingsystem.activities.api;

import java.time.Instant;
import java.util.UUID;

public record JobActivityResponse(UUID linkId, UUID jobId, ActivityResponse activity, Instant linkedAt,
		int employeesLinked) {}
