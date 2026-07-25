package dev.igorbarbosa.worktrainingsystem.employees.application;

import java.util.UUID;

/** Public employee lifecycle boundary implemented by the operational activity module. */
public interface EmployeeLifecyclePort {
	LifecycleEffects initialize(EmployeeData employee, UUID responsibleUserId);
	LifecycleEffects changeJob(EmployeeData employee, UUID previousJobId, boolean removePreviousJobActivities,
			UUID responsibleUserId);

	record EmployeeData(UUID id, UUID organizationId, UUID jobId, boolean active) {}
	record LifecycleEffects(int activitiesAdded, int activitiesRemoved, int assignmentsCreated,
			int qualificationsRecalculated) {
		public static LifecycleEffects none() { return new LifecycleEffects(0, 0, 0, 0); }
	}
}
