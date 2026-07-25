package dev.igorbarbosa.worktrainingsystem.qualifications.application;

import java.util.UUID;

public interface QualificationCommandPort {
	int recalculateEmployee(UUID employeeId);
	int recalculateActivity(UUID activityId);
}
