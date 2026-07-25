package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AssessmentAttemptRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.CompletionExpirationHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.TrainingCompliancePort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RepositoryTrainingComplianceAdapter implements TrainingCompliancePort {
	private final TrainingCompletionRepository completions;
	private final CompletionExpirationHistoryRepository expirationHistory;
	private final AssessmentAttemptRepository attempts;
	private final Clock clock;
	public RepositoryTrainingComplianceAdapter(TrainingCompletionRepository completions,
			CompletionExpirationHistoryRepository expirationHistory, AssessmentAttemptRepository attempts, Clock clock) {
		this.completions = completions; this.expirationHistory = expirationHistory; this.attempts = attempts; this.clock = clock;
	}

	@Override @Transactional(readOnly = true)
	public ComplianceStatus status(UUID employeeId, UUID trainingId) {
		LocalDate today = LocalDate.now(clock);
		TrainingCompletion selected = null; LocalDate selectedExpiration = null;
		for (TrainingCompletion completion : completions.findAllByOrganizationIdAndEmployeeIdAndTrainingIdOrderByCompletedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, employeeId, trainingId)) {
			LocalDate expiration = effectiveExpiration(completion);
			if (selected == null) { selected = completion; selectedExpiration = expiration; }
			if (expiration == null || !expiration.isBefore(today)) { selected = completion; selectedExpiration = expiration; break; }
		}
		var latestAttempt = attempts.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdOrderBySubmittedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, employeeId, trainingId).orElse(null);
		boolean unresolvedFailure = latestAttempt != null && latestAttempt.getResult() == AssessmentResult.FAILED
				&& (selected == null || latestAttempt.getSubmittedAt().isAfter(selected.getCompletedAt()));
		Optional<CompletionEvidence> evidence = selected == null ? Optional.empty()
				: Optional.of(new CompletionEvidence(selected.getId(), selected.getTrainingVersionId(),
						selected.getCompletedAt(), selectedExpiration));
		return new ComplianceStatus(evidence, unresolvedFailure);
	}

	private LocalDate effectiveExpiration(TrainingCompletion completion) {
		var latest = expirationHistory.findFirstByOrganizationIdAndCompletionIdOrderByCreatedAtDescIdDesc(
				DEFAULT_ORGANIZATION_ID, completion.getId()).orElse(null);
		return latest == null ? completion.getExpirationDate() : latest.getRecalculatedExpirationDate();
	}
}
