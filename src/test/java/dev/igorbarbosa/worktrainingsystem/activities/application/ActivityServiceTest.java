package dev.igorbarbosa.worktrainingsystem.activities.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import dev.igorbarbosa.worktrainingsystem.activities.api.ManualEmployeeActivityRequest;
import dev.igorbarbosa.worktrainingsystem.activities.api.RequirementRequest;
import dev.igorbarbosa.worktrainingsystem.activities.domain.Activity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.ActivityTrainingRequirement;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import dev.igorbarbosa.worktrainingsystem.activities.domain.JobActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityTrainingRequirementRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.EmployeeActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.JobActivityRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
	@Mock ActivityRepository activities;
	@Mock JobActivityRepository jobActivities;
	@Mock EmployeeActivityRepository employeeActivities;
	@Mock ActivityTrainingRequirementRepository requirements;
	@Mock EmployeeActivityCatalog employees;
	@Mock JobActivityCatalog jobs;
	@Mock TrainingCatalog trainings;
	@Mock AuthorizationService authorization;
	@Mock CurrentUserProvider currentUser;
	@Mock ApplicationEventPublisher events;
	@InjectMocks ActivityService service;

	private UUID employeeId;
	private UUID activityId;
	private UUID actorId;
	private Activity activity;

	@BeforeEach
	void setUp() {
		employeeId = UUID.randomUUID(); activityId = UUID.randomUUID(); actorId = UUID.randomUUID();
		activity = new Activity(DEFAULT_ORGANIZATION_ID, "Operar ponte", null, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(activity, "id", activityId);
	}

	@Test
	void mergesJobAndManualOriginsIntoOneEffectiveActivity() {
		when(authorization.canAccessEmployee(employeeId)).thenReturn(true);
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		EmployeeActivity job = link(EmployeeActivityOrigin.JOB, UUID.randomUUID(), null);
		EmployeeActivity manual = link(EmployeeActivityOrigin.MANUAL, null, "Autorização");
		when(employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
				DEFAULT_ORGANIZATION_ID, employeeId, RegistrationStatus.ACTIVE)).thenReturn(List.of(job, manual));

		var result = service.listEmployeeActivities(employeeId);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().origins()).containsExactlyInAnyOrder(EmployeeActivityOrigin.JOB, EmployeeActivityOrigin.MANUAL);
	}

	@Test
	void addingAnExistingManualOriginIsIdempotent() {
		EmployeeActivity manual = link(EmployeeActivityOrigin.MANUAL, null, "Autorização");
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		when(employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndOriginAndStatus(DEFAULT_ORGANIZATION_ID,
				employeeId, activityId, EmployeeActivityOrigin.MANUAL, RegistrationStatus.ACTIVE)).thenReturn(Optional.of(manual));
		when(employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(DEFAULT_ORGANIZATION_ID,
				employeeId, RegistrationStatus.ACTIVE)).thenReturn(List.of(manual));
		when(requirements.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(DEFAULT_ORGANIZATION_ID,
				activityId, RegistrationStatus.ACTIVE)).thenReturn(List.of());
		when(currentUser.requireCurrentUser()).thenReturn(currentUser());

		var result = service.addManualEmployeeActivity(employeeId, new ManualEmployeeActivityRequest(activityId, "Outra"));

		assertThat(result.origins()).containsExactly(EmployeeActivityOrigin.MANUAL);
		verify(employeeActivities, never()).saveAndFlush(any());
	}

	@Test
	void rejectsNewActivityForInactiveEmployee() {
		when(employees.requireActiveEmployee(employeeId)).thenThrow(
				new BusinessRuleViolationException("EMPLOYEE_INACTIVE", "inativo"));

		assertThatThrownBy(() -> service.addManualEmployeeActivity(employeeId,
				new ManualEmployeeActivityRequest(activityId, null)))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("EMPLOYEE_INACTIVE");
		verify(activities, never()).findByIdAndOrganizationId(any(), any());
	}

	@Test
	void validatesLatestPublishedPolicyAndEmitsAfterCommitRequests() {
		UUID trainingId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		doReturn(training(trainingId)).when(trainings).requireActiveTraining(trainingId);
		when(trainings.resolveLatestPublished(trainingId)).thenReturn(
				new TrainingCatalog.VersionSummary(versionId, trainingId, 2, TrainingVersionStatus.PUBLISHED));
		when(requirements.saveAndFlush(any())).thenAnswer(invocation -> {
			ActivityTrainingRequirement value = invocation.getArgument(0);
			ReflectionTestUtils.setField(value, "id", UUID.randomUUID()); return value;
		});
		when(employeeActivities.findActiveEmployeeIds(org.mockito.ArgumentMatchers.eq(DEFAULT_ORGANIZATION_ID),
				org.mockito.ArgumentMatchers.eq(activityId), any())).thenReturn(new PageImpl<>(List.of(employeeId)));
		when(currentUser.requireCurrentUser()).thenReturn(currentUser());

		service.addRequirement(activityId, new RequirementRequest(trainingId,
				RequirementVersionPolicy.LATEST_PUBLISHED, null, true, null));

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(events, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
		assertThat(captor.getAllValues()).anyMatch(ActivityAssignmentRequested.class::isInstance)
				.anyMatch(QualificationRecalculationRequested.class::isInstance);
	}

	@Test
	void rejectsOptionalRequirementWithoutConsultingCatalog() {
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		assertThatThrownBy(() -> service.addRequirement(activityId, new RequirementRequest(UUID.randomUUID(),
				RequirementVersionPolicy.LATEST_PUBLISHED, null, false, true)))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code")
				.isEqualTo("REQUIREMENT_MUST_BE_REQUIRED");
		verify(trainings, never()).requireActiveTraining(any());
	}

	@Test
	void rejectsInactiveTrainingAndFixedPolicyWithoutVersion() {
		UUID trainingId = UUID.randomUUID();
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		when(trainings.requireActiveTraining(trainingId)).thenThrow(
				new BusinessRuleViolationException("TRAINING_INACTIVE", "inativo"));
		assertThatThrownBy(() -> service.addRequirement(activityId, new RequirementRequest(trainingId,
				RequirementVersionPolicy.LATEST_PUBLISHED, null, true, true)))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("TRAINING_INACTIVE");

		doReturn(training(trainingId)).when(trainings).requireActiveTraining(trainingId);
		assertThatThrownBy(() -> service.addRequirement(activityId, new RequirementRequest(trainingId,
				RequirementVersionPolicy.FIXED_VERSION, null, true, true)))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("TRAINING_VERSION_REQUIRED");
	}

	@Test
	void removingJobDefaultPreservesIndependentManualOrigin() {
		UUID jobId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID();
		JobActivity source = new JobActivity(DEFAULT_ORGANIZATION_ID, jobId, activityId, actorId, Instant.now());
		ReflectionTestUtils.setField(source, "id", sourceId);
		EmployeeActivity job = link(EmployeeActivityOrigin.JOB, sourceId, null);
		EmployeeActivity manual = link(EmployeeActivityOrigin.MANUAL, null, "Independente");
		when(activities.findByIdAndOrganizationId(activityId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(activity));
		when(jobActivities.findByOrganizationIdAndJobIdAndActivityIdAndStatus(DEFAULT_ORGANIZATION_ID, jobId,
				activityId, RegistrationStatus.ACTIVE)).thenReturn(Optional.of(source));
		when(employeeActivities.findAllByOrganizationIdAndSourceJobActivityIdAndStatus(DEFAULT_ORGANIZATION_ID,
				sourceId, RegistrationStatus.ACTIVE)).thenReturn(List.of(job));
		when(currentUser.requireCurrentUser()).thenReturn(currentUser());

		service.removeJobActivity(jobId, activityId);

		assertThat(job.getStatus()).isEqualTo(RegistrationStatus.INACTIVE);
		assertThat(manual.getStatus()).isEqualTo(RegistrationStatus.ACTIVE);
		assertThat(source.getStatus()).isEqualTo(RegistrationStatus.INACTIVE);
	}

	private EmployeeActivity link(EmployeeActivityOrigin origin, UUID source, String reason) {
		EmployeeActivity link = new EmployeeActivity(DEFAULT_ORGANIZATION_ID, employeeId, activityId, origin,
				source, reason, actorId, Instant.parse("2026-07-24T12:00:00Z"));
		ReflectionTestUtils.setField(link, "id", UUID.randomUUID()); return link;
	}
	private CurrentUser currentUser() { return new CurrentUser(actorId, DEFAULT_ORGANIZATION_ID, UserRole.ADMIN, null, Set.of()); }
	private TrainingCatalog.TrainingSummary training(UUID id) {
		return new TrainingCatalog.TrainingSummary(id, "NR-11", "NR11", null, "NR", true, RegistrationStatus.ACTIVE);
	}
}
