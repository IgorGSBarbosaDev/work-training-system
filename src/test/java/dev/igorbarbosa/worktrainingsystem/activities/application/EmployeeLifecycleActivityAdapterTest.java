package dev.igorbarbosa.worktrainingsystem.activities.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.activities.domain.ActivityTrainingRequirement;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import dev.igorbarbosa.worktrainingsystem.activities.domain.JobActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityTrainingRequirementRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.EmployeeActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.JobActivityRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeLifecyclePort;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeLifecycleActivityAdapterTest {
	@Mock JobActivityRepository jobActivities;
	@Mock EmployeeActivityRepository employeeActivities;
	@Mock ActivityTrainingRequirementRepository requirements;
	@Mock AssignmentGenerationPort assignments;
	@Mock QualificationCommandPort qualifications;

	@Test
	void initializesJobActivitiesAssignmentsAndQualificationsForActiveEmployee() {
		UUID employeeId = UUID.randomUUID(); UUID jobId = UUID.randomUUID(); UUID activityId = UUID.randomUUID();
		UUID actor = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); UUID requirementId = UUID.randomUUID();
		JobActivity source = new JobActivity(DEFAULT_ORGANIZATION_ID, jobId, activityId, actor, Instant.now());
		ReflectionTestUtils.setField(source, "id", sourceId);
		EmployeeActivity activeLink = new EmployeeActivity(DEFAULT_ORGANIZATION_ID, employeeId, activityId,
				EmployeeActivityOrigin.JOB, sourceId, null, actor, Instant.now());
		ActivityTrainingRequirement requirement = new ActivityTrainingRequirement(DEFAULT_ORGANIZATION_ID, activityId,
				UUID.randomUUID(), RequirementVersionPolicy.LATEST_PUBLISHED, null, actor, Instant.now());
		ReflectionTestUtils.setField(requirement, "id", requirementId);
		when(employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
				DEFAULT_ORGANIZATION_ID, employeeId, RegistrationStatus.ACTIVE)).thenReturn(List.of(), List.of(activeLink));
		when(jobActivities.findAllByOrganizationIdAndJobIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, jobId, RegistrationStatus.ACTIVE)).thenReturn(List.of(source));
		when(employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndSourceJobActivityIdAndStatus(
				DEFAULT_ORGANIZATION_ID, employeeId, activityId, sourceId, RegistrationStatus.ACTIVE)).thenReturn(Optional.empty());
		when(requirements.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, activityId, RegistrationStatus.ACTIVE)).thenReturn(List.of(requirement));
		when(assignments.generate(any())).thenReturn(1);
		when(qualifications.recalculateEmployee(employeeId)).thenReturn(1);
		var adapter = new EmployeeLifecycleActivityAdapter(jobActivities, employeeActivities, requirements,
				assignments, qualifications);

		var result = adapter.initialize(new EmployeeLifecyclePort.EmployeeData(employeeId,
				DEFAULT_ORGANIZATION_ID, jobId, true), actor);

		assertThat(result.activitiesAdded()).isEqualTo(1);
		assertThat(result.assignmentsCreated()).isEqualTo(1);
		assertThat(result.qualificationsRecalculated()).isEqualTo(1);
		verify(employeeActivities).saveAndFlush(any(EmployeeActivity.class));
	}

	@Test
	void skipsInactiveEmployeeWithoutDerivedWrites() {
		var adapter = new EmployeeLifecycleActivityAdapter(jobActivities, employeeActivities, requirements,
				assignments, qualifications);
		var result = adapter.initialize(new EmployeeLifecyclePort.EmployeeData(UUID.randomUUID(),
				DEFAULT_ORGANIZATION_ID, UUID.randomUUID(), false), UUID.randomUUID());
		assertThat(result).isEqualTo(EmployeeLifecyclePort.LifecycleEffects.none());
		verify(employeeActivities, never()).saveAndFlush(any());
	}
}
