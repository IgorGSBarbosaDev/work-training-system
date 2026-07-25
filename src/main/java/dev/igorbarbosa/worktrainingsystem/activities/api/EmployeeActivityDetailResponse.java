package dev.igorbarbosa.worktrainingsystem.activities.api;

import java.util.List;

public record EmployeeActivityDetailResponse(EmployeeActivityResponse activity,
		List<RequirementResponse> requirements) {}
