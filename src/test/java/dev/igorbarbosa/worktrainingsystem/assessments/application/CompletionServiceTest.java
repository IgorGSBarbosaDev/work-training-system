package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.assessments.api.ManualCompletionRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionForm;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.CompletionExpirationHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort.ExecutionAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeActivityCatalog;
import dev.igorbarbosa.worktrainingsystem.files.application.UploadedFileCatalog;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CompletionServiceTest {
	@Mock TrainingCompletionRepository completions;
	@Mock CompletionExpirationHistoryRepository history;
	@Mock EmployeeActivityCatalog employees;
	@Mock TrainingCatalog trainings;
	@Mock UploadedFileCatalog files;
	@Mock AuthorizationService authorization;
	@Mock CurrentUserProvider currentUser;
	@Mock ApplicationEventPublisher events;
	private final Instant now = Instant.parse("2024-01-31T12:00:00Z");

	@Test
	void validitySupportsDaysMonthEndLeapYearAndIndefinite() {
		LocalDate leapDay = LocalDate.of(2024, 1, 31);
		assertThat(CompletionService.expiration(leapDay, ValidityType.DAYS, 30)).isEqualTo("2024-03-01");
		assertThat(CompletionService.expiration(leapDay, ValidityType.MONTHS, 1)).isEqualTo("2024-02-29");
		assertThat(CompletionService.expiration(LocalDate.of(2023, 1, 31), ValidityType.MONTHS, 1)).isEqualTo("2023-02-28");
		assertThat(CompletionService.expiration(leapDay, ValidityType.INDEFINITE, null)).isNull();
	}

	@Test
	void automaticCompletionIsIdempotentAndNoQuestionnaireReadinessCompletes() {
		ExecutionAssignment assignment = assignment(); TrainingCompletion existing = automatic(assignment);
		when(trainings.completionRules(assignment.trainingId(), assignment.trainingVersionId())).thenReturn(rules());
		when(completions.insertAutomaticIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(0);
		when(completions.findByOrganizationIdAndSourceAssignmentId(DEFAULT_ORGANIZATION_ID, assignment.id()))
				.thenReturn(Optional.of(existing));
		assertThat(service().contentReady(assignment)).isTrue();
		verify(events, never()).publishEvent(any());
	}

	@Test
	void manualCompletionAcceptsHistoricalExactVersionAndCompletedExternalCertificate() {
		UUID employee = UUID.randomUUID(), training = UUID.randomUUID(), version = UUID.randomUUID(), file = UUID.randomUUID();
		when(trainings.completionRules(training, version)).thenReturn(new TrainingCatalog.CompletionRules(training, version, 1,
				TrainingVersionStatus.ARCHIVED, ValidityType.MONTHS, 24, new BigDecimal("70.00"), 3, 10));
		when(currentUser.requireCurrentUser()).thenReturn(new CurrentUser(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID,
				UserRole.ADMIN, null, Set.of()));
		when(completions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		ManualCompletionRequest request = new ManualCompletionRequest(employee, training, version, now.minusSeconds(60),
				new BigDecimal("85.00"), ValidityType.INDEFINITE, null, "External", file);
		var response = service().manual(request);
		assertThat(response.form()).isEqualTo(CompletionForm.MANUAL);
		assertThat(response.trainingVersionId()).isEqualTo(version);
		assertThat(response.expirationDate()).isNull();
		verify(files).requireExternalCertificate(file, employee);
		verify(trainings).completionRules(training, version);
	}

	@Test
	void invalidManualValidityIsRejected() {
		UUID employee = UUID.randomUUID(), training = UUID.randomUUID(), version = UUID.randomUUID();
		when(trainings.completionRules(training, version)).thenReturn(new TrainingCatalog.CompletionRules(training, version, 1,
				TrainingVersionStatus.PUBLISHED, ValidityType.MONTHS, 12, new BigDecimal("70.00"), 3, 10));
		assertThatThrownBy(() -> service().manual(new ManualCompletionRequest(employee, training, version,
				now.minusSeconds(1), null, ValidityType.DAYS, null, null, null)))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("validade");
	}

	private CompletionService service() { return new CompletionService(completions, history, employees, trainings, files,
			authorization, currentUser, events, Clock.fixed(now, ZoneOffset.UTC)); }
	private ExecutionAssignment assignment() { return new ExecutionAssignment(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID,
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AssignmentStatus.IN_PROGRESS); }
	private TrainingCatalog.CompletionRules rules() { return new TrainingCatalog.CompletionRules(UUID.randomUUID(), UUID.randomUUID(), 1,
			TrainingVersionStatus.PUBLISHED, ValidityType.MONTHS, 12, new BigDecimal("70.00"), 3, 10); }
	private TrainingCompletion automatic(ExecutionAssignment assignment) { return new TrainingCompletion(DEFAULT_ORGANIZATION_ID,
			assignment.employeeId(), assignment.trainingId(), assignment.trainingVersionId(), assignment.id(), now,
			CompletionForm.AUTOMATIC, null, ValidityType.MONTHS, 12, LocalDate.of(2025, 1, 31), null, null, null); }
}
