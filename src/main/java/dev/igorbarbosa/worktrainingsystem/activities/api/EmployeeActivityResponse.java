package dev.igorbarbosa.worktrainingsystem.activities.api;

import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record EmployeeActivityResponse(UUID employeeId, ActivityResponse activity,
		Set<EmployeeActivityOrigin> origins, Instant assignedAt, boolean effective) {}
