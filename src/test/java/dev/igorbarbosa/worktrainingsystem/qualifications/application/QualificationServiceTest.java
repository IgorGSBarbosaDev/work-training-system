package dev.igorbarbosa.worktrainingsystem.qualifications.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityOperationsCatalog;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentStatusPort;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.organizations.application.QualificationSettingsCatalog;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.ActivityQualification;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationBlockingType;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import dev.igorbarbosa.worktrainingsystem.qualifications.persistence.ActivityQualificationRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualificationServiceTest {
	@Mock ActivityQualificationRepository repository;
	@Mock ActivityOperationsCatalog activities;
	@Mock EmployeeActivityCatalog employees;
	@Mock TrainingCatalog trainings;
	@Mock TrainingCompliancePort compliance;
	@Mock AssignmentStatusPort assignmentStatuses;
	@Mock QualificationSettingsCatalog settings;
	@Mock AuthorizationService authorization;
	private QualificationService service;
	private final UUID employeeId = UUID.randomUUID();
	private final UUID activityId = UUID.randomUUID();
	private final UUID trainingId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();
	private final UUID requirementId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-07-24T12:00:00Z");
	private final AtomicReference<ActivityQualification> persisted = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
		service = new QualificationService(repository, activities, employees, trainings, compliance,
				assignmentStatuses, settings, authorization, mapper, Clock.fixed(now, ZoneOffset.UTC));
		when(employees.requireEmployee(employeeId)).thenReturn(new EmployeeActivityCatalog.EmployeeSummary(employeeId,
				"Ana", "100", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true));
		when(activities.requireActivity(activityId)).thenReturn(new ActivityOperationsCatalog.ActivitySummary(activityId, "Operar ponte", true));
		when(settings.expiringSoonDays(DEFAULT_ORGANIZATION_ID)).thenReturn(30);
		when(repository.upsert(any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
			ActivityQualification value = new TestQualification();
			ReflectionTestUtils.setField(value, "id", invocation.getArgument(0));
			ReflectionTestUtils.setField(value, "organizationId", invocation.getArgument(1));
			ReflectionTestUtils.setField(value, "employeeId", invocation.getArgument(2));
			ReflectionTestUtils.setField(value, "activityId", invocation.getArgument(3));
			ReflectionTestUtils.setField(value, "status", QualificationStatus.valueOf(invocation.getArgument(4)));
			ReflectionTestUtils.setField(value, "calculatedAt", invocation.getArgument(5));
			ReflectionTestUtils.setField(value, "nextExpirationDate", invocation.getArgument(6));
			ReflectionTestUtils.setField(value, "blockingReasons", invocation.getArgument(7));
			persisted.set(value); return 1;
		});
		when(repository.findByOrganizationIdAndEmployeeIdAndActivityId(DEFAULT_ORGANIZATION_ID, employeeId, activityId))
				.thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
	}

	@Test
	void persistsNotAssignedWithoutInspectingCompliance() {
		when(activities.isEffectivelyAssigned(employeeId, activityId)).thenReturn(false);
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.NOT_ASSIGNED);
		assertThat(result.blockingReasons()).isEmpty();
		assertThat(result.disclaimer()).isEqualTo(dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse.DISCLAIMER);
	}

	@Test
	void blocksConservativelyWhenCompletionIsMissingAndWaiverIsNotEffective() {
		stubRequirement(RequirementVersionPolicy.FIXED_VERSION);
		when(compliance.status(employeeId, trainingId)).thenReturn(TrainingCompliancePort.ComplianceStatus.missing());
		when(assignmentStatuses.effectiveStatus(employeeId, trainingId, versionId)).thenReturn(Optional.empty());
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.BLOCKED);
		assertThat(result.blockingReasons()).singleElement().satisfies(reason -> {
			assertThat(reason.type()).isEqualTo(QualificationBlockingType.MISSING_TRAINING);
			assertThat(reason.assignmentStatus()).isNull();
		});
	}

	@Test
	void blocksExpiredCompletion() {
		stubRequirement(RequirementVersionPolicy.FIXED_VERSION);
		stubCompletion(versionId, LocalDate.of(2026, 7, 23), false);
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.BLOCKED);
		assertThat(result.blockingReasons()).extracting(reason -> reason.type())
				.containsExactly(QualificationBlockingType.EXPIRED_TRAINING);
	}

	@Test
	void blocksUnresolvedFailedAssessment() {
		stubRequirement(RequirementVersionPolicy.FIXED_VERSION);
		stubCompletion(versionId, LocalDate.of(2027, 1, 1), true);
		when(assignmentStatuses.effectiveStatus(employeeId, trainingId, versionId)).thenReturn(Optional.of(AssignmentStatus.FAILED));
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.BLOCKED);
		assertThat(result.blockingReasons().getFirst().type()).isEqualTo(QualificationBlockingType.FAILED_ASSESSMENT);
	}

	@Test
	void expiringWindowIncludesTheConfiguredThirtyDayBoundary() {
		stubRequirement(RequirementVersionPolicy.FIXED_VERSION);
		stubCompletion(versionId, LocalDate.of(2026, 8, 23), false);
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.EXPIRING);
		assertThat(result.nextExpirationDate()).isEqualTo(LocalDate.of(2026, 8, 23));
	}

	@Test
	void availableWhenEveryRequirementIsValidBeyondWindow() {
		stubRequirement(RequirementVersionPolicy.FIXED_VERSION);
		stubCompletion(versionId, LocalDate.of(2026, 8, 24), false);
		assertThat(service.calculate(employeeId, activityId).status()).isEqualTo(QualificationStatus.AVAILABLE);
	}

	@Test
	void latestPolicyRequiresCompletionOfCurrentPublishedVersion() {
		UUID oldVersion = UUID.randomUUID();
		stubRequirement(RequirementVersionPolicy.LATEST_PUBLISHED);
		stubCompletion(oldVersion, LocalDate.of(2027, 1, 1), false);
		var result = service.calculate(employeeId, activityId);
		assertThat(result.status()).isEqualTo(QualificationStatus.BLOCKED);
		assertThat(result.blockingReasons().getFirst().requiredVersionId()).isEqualTo(versionId);
		assertThat(result.blockingReasons().getFirst().completionVersionId()).isEqualTo(oldVersion);
	}

	private void stubRequirement(RequirementVersionPolicy policy) {
		when(activities.isEffectivelyAssigned(employeeId, activityId)).thenReturn(true);
		UUID fixed = policy == RequirementVersionPolicy.FIXED_VERSION ? versionId : null;
		when(activities.activeRequirements(activityId)).thenReturn(List.of(
				new ActivityOperationsCatalog.RequirementSummary(requirementId, activityId, trainingId, policy, fixed)));
		when(trainings.summary(trainingId)).thenReturn(new TrainingCatalog.TrainingSummary(trainingId, "NR-11", "NR11",
				null, "NR", true, RegistrationStatus.ACTIVE));
		if (policy == RequirementVersionPolicy.FIXED_VERSION) {
			when(trainings.historicalVersion(trainingId, versionId)).thenReturn(version(versionId));
		} else {
			when(trainings.latestPublishedForCompliance(trainingId)).thenReturn(version(versionId));
		}
	}

	private void stubCompletion(UUID completionVersion, LocalDate expiration, boolean failed) {
		when(compliance.status(employeeId, trainingId)).thenReturn(new TrainingCompliancePort.ComplianceStatus(
				Optional.of(new TrainingCompliancePort.CompletionEvidence(UUID.randomUUID(), completionVersion,
						now.minusSeconds(3600), expiration)), failed));
		when(assignmentStatuses.effectiveStatus(employeeId, trainingId, versionId)).thenReturn(Optional.of(AssignmentStatus.NOT_STARTED));
	}

	private TrainingCatalog.VersionSummary version(UUID id) {
		return new TrainingCatalog.VersionSummary(id, trainingId, 2, TrainingVersionStatus.PUBLISHED);
	}

	private static final class TestQualification extends ActivityQualification {}
}
