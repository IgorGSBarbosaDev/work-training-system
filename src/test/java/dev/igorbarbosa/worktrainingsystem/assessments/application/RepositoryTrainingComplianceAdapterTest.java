package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentAttempt;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionForm;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AssessmentAttemptRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.CompletionExpirationHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryTrainingComplianceAdapterTest {
	@Mock TrainingCompletionRepository completions;
	@Mock CompletionExpirationHistoryRepository history;
	@Mock AssessmentAttemptRepository attempts;
	private final Instant now = Instant.parse("2026-07-24T12:00:00Z");

	@Test
	void reportsMissingValidExpiredAndUnresolvedLatestFailure() {
		UUID employee = UUID.randomUUID(), training = UUID.randomUUID();
		stub(employee, training, List.of(), Optional.empty());
		assertThat(adapter().status(employee, training).completion()).isEmpty();

		TrainingCompletion valid = completion(employee, training, now.minusSeconds(3600), LocalDate.of(2026, 8, 1));
		stub(employee, training, List.of(valid), Optional.empty());
		assertThat(adapter().status(employee, training).completion()).isPresent();

		TrainingCompletion expired = completion(employee, training, now.minusSeconds(7200), LocalDate.of(2026, 7, 1));
		AssessmentAttempt failed = new AssessmentAttempt(DEFAULT_ORGANIZATION_ID, UUID.randomUUID(), employee, training,
				UUID.randomUUID(), UUID.randomUUID(), 1, now.minusSeconds(60), new BigDecimal("50.00"),
				new BigDecimal("70.00"), AssessmentResult.FAILED, "key", "hash");
		stub(employee, training, List.of(expired), Optional.of(failed));
		var status = adapter().status(employee, training);
		assertThat(status.completion()).isPresent();
		assertThat(status.completion().orElseThrow().expirationDate()).isEqualTo("2026-07-01");
		assertThat(status.unresolvedFailedAssessment()).isTrue();
	}

	private void stub(UUID employee, UUID training, List<TrainingCompletion> values, Optional<AssessmentAttempt> attempt) {
		when(completions.findAllByOrganizationIdAndEmployeeIdAndTrainingIdOrderByCompletedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, employee, training)).thenReturn(values);
		when(attempts.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdOrderBySubmittedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, employee, training)).thenReturn(attempt);
		for (TrainingCompletion completion : values) when(history.findFirstByOrganizationIdAndCompletionIdOrderByCreatedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, completion.getId())).thenReturn(Optional.empty());
	}
	private RepositoryTrainingComplianceAdapter adapter() { return new RepositoryTrainingComplianceAdapter(completions, history,
			attempts, Clock.fixed(now, ZoneOffset.UTC)); }
	private TrainingCompletion completion(UUID employee, UUID training, Instant completed, LocalDate expires) {
		return new TrainingCompletion(DEFAULT_ORGANIZATION_ID, employee, training, UUID.randomUUID(), null, completed,
				CompletionForm.MANUAL, null, ValidityType.DAYS, 1, expires, UUID.randomUUID(), null, null);
	}
}
