package dev.igorbarbosa.worktrainingsystem.assignments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityAssignmentRequested;
import dev.igorbarbosa.worktrainingsystem.activities.application.ActivityOperationsCatalog;
import dev.igorbarbosa.worktrainingsystem.activities.domain.RequirementVersionPolicy;
import dev.igorbarbosa.worktrainingsystem.assignments.api.BatchAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.api.CreateAssignmentRequest;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatch;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentBatchResult;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentBatchRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentBatchResultRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentSourceRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.TrainingCompliancePort;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {
	@Mock TrainingAssignmentRepository assignments;
	@Mock AssignmentSourceRepository sources;
	@Mock AssignmentBatchRepository batches;
	@Mock AssignmentBatchResultRepository batchResults;
	@Mock EmployeeActivityCatalog employees;
	@Mock ActivityOperationsCatalog activities;
	@Mock TrainingCatalog trainings;
	@Mock TrainingCompliancePort compliance;
	@Mock QualificationCommandPort qualifications;
	@Mock AuthorizationService authorization;
	@Mock CurrentUserProvider currentUser;
	private AssignmentService service;
	private final UUID actorId = UUID.randomUUID();
	private final UUID employeeId = UUID.randomUUID();
	private final UUID trainingId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();
	private final UUID jobId = UUID.randomUUID();
	private final UUID sectorId = UUID.randomUUID();
	private final UUID unitId = UUID.randomUUID();
	private final AtomicReference<TrainingAssignment> inserted = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		service = new AssignmentService(assignments, sources, batches, batchResults, employees, activities,
				trainings, compliance, qualifications, authorization, currentUser);
		when(currentUser.requireCurrentUser()).thenReturn(new CurrentUser(actorId, DEFAULT_ORGANIZATION_ID,
				UserRole.ADMIN, null, Set.of()));
		when(authorization.canAccessEmployee(any())).thenReturn(true);
		when(authorization.currentScope()).thenReturn(new AuthorizationService.AccessScope(actorId,
				DEFAULT_ORGANIZATION_ID, UserRole.ADMIN, null, true, Set.of(), Set.of(), Set.of()));
		when(employees.requireActiveEmployee(employeeId)).thenReturn(employee(employeeId, true));
		when(employees.requireEmployee(employeeId)).thenReturn(employee(employeeId, true));
		when(trainings.requireActiveTraining(trainingId)).thenReturn(training());
		when(trainings.resolveLatestPublished(trainingId)).thenReturn(version());
		when(trainings.requirePublishedVersion(trainingId, versionId)).thenReturn(version());
		when(trainings.historicalVersion(trainingId, versionId)).thenReturn(version());
		when(trainings.summary(trainingId)).thenReturn(training());
		when(sources.findAllByOrganizationIdAndAssignmentIdOrderByCreatedAt(any(), any())).thenReturn(List.of());
		when(assignments.insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(Boolean.class), any(), any(), any(), any())).thenAnswer(invocation -> {
			TrainingAssignment value = new TrainingAssignment(invocation.getArgument(1), invocation.getArgument(2),
					invocation.getArgument(3), invocation.getArgument(4), AssignmentOrigin.valueOf(invocation.getArgument(5)),
					invocation.getArgument(6), invocation.getArgument(8), AssignmentPriority.valueOf(invocation.getArgument(9)),
					invocation.getArgument(10), invocation.getArgument(12), invocation.getArgument(13),
					invocation.getArgument(14), invocation.getArgument(15));
			ReflectionTestUtils.setField(value, "id", invocation.getArgument(0));
			ReflectionTestUtils.setField(value, "createdAt", invocation.getArgument(6));
			ReflectionTestUtils.setField(value, "updatedAt", invocation.getArgument(6));
			inserted.set(value); return 1;
		});
		when(assignments.findByIdAndOrganizationId(any(), eq(DEFAULT_ORGANIZATION_ID)))
				.thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
	}

	@ParameterizedTest
	@MethodSource("origins")
	void storesEveryManualOriginWithSourceProvenance(AssignmentOrigin origin, UUID requestedSource) {
		var result = service.create(new CreateAssignmentRequest(employeeId, trainingId, null, origin,
				requestedSource, LocalDate.now().plusDays(10), AssignmentPriority.HIGH, null), null);
		assertThat(result.origin()).isEqualTo(origin);
		UUID expectedSource = switch (origin) {
			case EMPLOYEE -> employeeId; case JOB -> jobId; case SECTOR -> sectorId; case UNIT -> unitId;
			case ACTIVITY, GROUP -> requestedSource; default -> throw new IllegalStateException();
		};
		verify(sources).insertIfAbsent(any(), eq(DEFAULT_ORGANIZATION_ID), eq(result.id()), eq(origin.name()),
				eq(expectedSource), any());
	}

	@Test
	void rejectsInactiveEmployeeAndOutOfScopeEmployee() {
		when(authorization.canAccessEmployee(employeeId)).thenReturn(false);
		assertThatThrownBy(() -> service.create(request(AssignmentOrigin.EMPLOYEE, null), null))
				.isInstanceOf(AccessDeniedException.class);
		when(authorization.canAccessEmployee(employeeId)).thenReturn(true);
		when(employees.requireActiveEmployee(employeeId)).thenThrow(new BusinessRuleViolationException("EMPLOYEE_INACTIVE", "inativo"));
		assertThatThrownBy(() -> service.create(request(AssignmentOrigin.EMPLOYEE, null), null))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("EMPLOYEE_INACTIVE");
	}

	@Test
	void sameIdempotencyKeyReturnsOriginalAssignment() {
		var request = new CreateAssignmentRequest(employeeId, trainingId, null, AssignmentOrigin.EMPLOYEE,
				null, null, AssignmentPriority.NORMAL, "same-key");
		var first = service.create(request, null);
		when(assignments.findByOrganizationIdAndResponsibleUserIdAndIdempotencyKey(
				DEFAULT_ORGANIZATION_ID, actorId, "same-key")).thenReturn(Optional.of(inserted.get()));
		var second = service.create(request, null);
		assertThat(second.id()).isEqualTo(first.id());
	}

	@Test
	void translatesConcurrentEffectiveDuplicate() {
		org.mockito.Mockito.doReturn(0).when(assignments).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(Boolean.class), any(), any(), any(), any());
		when(assignments.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdAndTrainingVersionIdAndStatusIn(
				any(), any(), any(), any(), any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.create(request(AssignmentOrigin.EMPLOYEE, null), null))
				.isInstanceOf(ResourceConflictException.class).extracting("code").isEqualTo("CONCURRENT_ASSIGNMENT_CONFLICT");
	}

	@Test
	void duplicateAutomaticActivityOriginAddsProvenanceWithoutAnotherAssignment() {
		TrainingAssignment existing = existingAssignment();
		org.mockito.Mockito.doReturn(0).when(assignments).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(Boolean.class), any(), any(), any(), any());
		when(assignments.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdAndTrainingVersionIdAndStatusIn(
				any(), any(), any(), any(), any())).thenReturn(Optional.of(existing));
		UUID requirementId = UUID.randomUUID();
		int created = service.generate(new ActivityAssignmentRequested(DEFAULT_ORGANIZATION_ID, Set.of(employeeId),
				UUID.randomUUID(), requirementId, trainingId, RequirementVersionPolicy.FIXED_VERSION, versionId, actorId));
		assertThat(created).isZero();
		verify(sources).insertIfAbsent(any(), eq(DEFAULT_ORGANIZATION_ID), eq(existing.getId()),
				eq(AssignmentOrigin.ACTIVITY.name()), eq(requirementId), any());
	}

	@Test
	void batchReportsCreatedAndInactiveEmployees() {
		UUID inactiveId = UUID.randomUUID(); List<AssignmentBatchResult> savedResults = new ArrayList<>();
		when(employees.requireEmployee(inactiveId)).thenReturn(employee(inactiveId, false));
		when(batchResults.save(any())).thenAnswer(invocation -> { savedResults.add(invocation.getArgument(0)); return invocation.getArgument(0); });
		when(batchResults.findAllByOrganizationIdAndBatchIdOrderByEmployeeId(any(), any())).thenAnswer(invocation -> savedResults);
		when(batches.saveAndFlush(any())).thenAnswer(invocation -> {
			AssignmentBatch batch = invocation.getArgument(0); ReflectionTestUtils.setField(batch, "id", UUID.randomUUID()); return batch;
		});
		var request = new BatchAssignmentRequest(trainingId, null,
				new BatchAssignmentRequest.Target(AssignmentOrigin.GROUP, null, null, null, null, null,
						List.of(employeeId, inactiveId)), null, AssignmentPriority.NORMAL, false, true, null);
		var result = service.createBatch(request, null);
		assertThat(result.requested()).isEqualTo(2);
		assertThat(result.created()).isEqualTo(1);
		assertThat(result.failed()).isEqualTo(1);
		assertThat(result.results()).extracting(item -> item.code()).contains("EMPLOYEE_INACTIVE");
	}

	@Test
	void recyclesTerminalAssignmentAgainstLatestPublishedVersion() {
		TrainingAssignment previous = existingAssignment();
		ReflectionTestUtils.setField(previous, "status", AssignmentStatus.EXPIRED);
		when(assignments.findOne(any(Specification.class))).thenReturn(Optional.of(previous));
		var result = service.recycle(previous.getId(), "recycle-key");
		assertThat(result.origin()).isEqualTo(AssignmentOrigin.RECERTIFICATION);
		assertThat(result.recertification()).isTrue();
		assertThat(result.recertificationOfAssignmentId()).isEqualTo(previous.getId());
		verify(sources).insertIfAbsent(any(), eq(DEFAULT_ORGANIZATION_ID), eq(result.id()),
				eq(AssignmentOrigin.RECERTIFICATION.name()), eq(previous.getId()), any());
	}

	private static Stream<Arguments> origins() {
		UUID activity = UUID.randomUUID(); UUID group = UUID.randomUUID();
		return Stream.of(
				Arguments.of(AssignmentOrigin.EMPLOYEE, null),
				Arguments.of(AssignmentOrigin.JOB, null),
				Arguments.of(AssignmentOrigin.ACTIVITY, activity),
				Arguments.of(AssignmentOrigin.SECTOR, null),
				Arguments.of(AssignmentOrigin.UNIT, null),
				Arguments.of(AssignmentOrigin.GROUP, group));
	}

	private CreateAssignmentRequest request(AssignmentOrigin origin, UUID source) {
		return new CreateAssignmentRequest(employeeId, trainingId, null, origin, source, null, AssignmentPriority.NORMAL, null);
	}
	private EmployeeActivityCatalog.EmployeeSummary employee(UUID id, boolean active) {
		return new EmployeeActivityCatalog.EmployeeSummary(id, "Ana", "100", jobId, sectorId, unitId, active);
	}
	private TrainingCatalog.TrainingSummary training() {
		return new TrainingCatalog.TrainingSummary(trainingId, "NR-11", "NR11", null, "NR", true, RegistrationStatus.ACTIVE);
	}
	private TrainingCatalog.VersionSummary version() {
		return new TrainingCatalog.VersionSummary(versionId, trainingId, 1, TrainingVersionStatus.PUBLISHED);
	}
	private TrainingAssignment existingAssignment() {
		TrainingAssignment value = new TrainingAssignment(DEFAULT_ORGANIZATION_ID, employeeId, trainingId, versionId,
				AssignmentOrigin.EMPLOYEE, Instant.now(), null, AssignmentPriority.NORMAL, actorId, null, null, null, null);
		ReflectionTestUtils.setField(value, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(value, "createdAt", Instant.now());
		ReflectionTestUtils.setField(value, "updatedAt", Instant.now());
		return value;
	}
}
