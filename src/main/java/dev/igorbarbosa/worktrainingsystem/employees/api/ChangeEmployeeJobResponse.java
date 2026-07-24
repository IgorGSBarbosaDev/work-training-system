package dev.igorbarbosa.worktrainingsystem.employees.api;

import java.util.UUID;

public record ChangeEmployeeJobResponse(
		UUID employeeId,
		UUID previousJobId,
		UUID currentJobId,
		int activitiesAdded,
		int activitiesRemoved,
		int assignmentsCreated,
		int qualificationsRecalculated) {
}
