package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.api.CompletionResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentAttempt;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionForm;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AssessmentAttemptRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AttemptAnswerRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.progress.application.ContentProgressPort;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionContent;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionModule;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionOption;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionQuestion;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionQuestionnaire;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionQuestionnaireDetail;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {
	@Mock AssessmentAttemptRepository attempts;
	@Mock AttemptAnswerRepository answers;
	@Mock AssignmentExecutionPort assignments;
	@Mock TrainingExecutionCatalog trainings;
	@Mock ContentProgressPort progress;
	@Mock CompletionService completions;
	@Mock AuthorizationService authorization;
	@Mock ApplicationEventPublisher events;
	private final Instant now = Instant.parse("2026-07-24T12:00:00Z");
	private UUID assignmentId; private UUID employeeId; private UUID trainingId; private UUID versionId; private UUID questionnaireId;
	private AssignmentExecutionPort.ExecutionAssignment assignment;

	@BeforeEach
	void setUp() {
		assignmentId = UUID.randomUUID(); employeeId = UUID.randomUUID(); trainingId = UUID.randomUUID();
		versionId = UUID.randomUUID(); questionnaireId = UUID.randomUUID();
		assignment = assignment(AssignmentStatus.AWAITING_ASSESSMENT);
	}

	@Test
	void deliveryAndOpenApiShapeNeverContainCorrectnessAndShuffleIsStablePerAttempt() throws Exception {
		ExecutionQuestionnaireDetail questionnaire = questionnaire(5, 3, 10, true);
		when(assignments.requireOwner(assignmentId, false)).thenReturn(assignment);
		when(trainings.requireQuestionnaire(versionId, questionnaireId)).thenReturn(questionnaire);
		when(progress.requiredContentReady(assignmentId, versionId)).thenReturn(true);
		when(attempts.findFirstByOrganizationIdAndAssignmentIdAndQuestionnaireIdOrderByAttemptNumberDesc(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId)).thenReturn(Optional.empty());
		AssessmentService service = service();
		var first = service.questionnaire(assignmentId, questionnaireId);
		var second = service.questionnaire(assignmentId, questionnaireId);
		assertThat(first.questions()).extracting(item -> item.id()).containsExactlyElementsOf(
				second.questions().stream().map(item -> item.id()).toList());
		String json = new ObjectMapper().writeValueAsString(first);
		assertThat(json).doesNotContain("correct", "isCorrect", "answerKey");
	}

	@Test
	void rejectsDuplicateMissingAndOptionFromAnotherQuestion() {
		ExecutionQuestionnaireDetail questionnaire = questionnaire(2, 3, 0, false);
		stubSubmission(questionnaire);
		ExecutionQuestion first = questionnaire.questions().get(0); ExecutionQuestion second = questionnaire.questions().get(1);
		assertThatThrownBy(() -> service().submit(assignmentId, questionnaireId, request(
				answer(first, 0), answer(first, 1)), "duplicate"))
				.isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("uma única vez");
		assertThatThrownBy(() -> service().submit(assignmentId, questionnaireId, request(answer(first, 0)), "missing"))
				.isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("exatamente uma resposta");
		var wrong = new AssessmentAttemptRequest.Answer(first.id(), second.options().get(0).id());
		assertThatThrownBy(() -> service().submit(assignmentId, questionnaireId, request(wrong, answer(second, 0)), "wrong"))
				.isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("não pertence");
	}

	@Test
	void scoreUsesStableTwoDecimalBoundary() {
		assertThat(AssessmentService.score(6999, 10000)).isEqualByComparingTo("69.99");
		assertThat(AssessmentService.score(7, 10)).isEqualByComparingTo("70.00");
	}

	@Test
	void enforcesMaximumAndAllowsRetryAtExactBoundary() {
		ExecutionQuestionnaireDetail questionnaire = questionnaire(2, 2, 10, false);
		AssessmentAttempt last = attempt(2, now.minusSeconds(600), AssessmentResult.FAILED);
		when(assignments.requireOwner(assignmentId, false)).thenReturn(assignment);
		when(trainings.requireQuestionnaire(versionId, questionnaireId)).thenReturn(questionnaire);
		when(progress.requiredContentReady(assignmentId, versionId)).thenReturn(true);
		when(attempts.findFirstByOrganizationIdAndAssignmentIdAndQuestionnaireIdOrderByAttemptNumberDesc(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId)).thenReturn(Optional.of(last));
		assertThat(service().availability(assignmentId, questionnaireId).available()).isFalse();
		assertThat(service().availability(assignmentId, questionnaireId).attemptsRemaining()).isZero();

		ExecutionQuestionnaireDetail threeAttempts = new ExecutionQuestionnaireDetail(questionnaire.id(), versionId,
				questionnaire.title(), questionnaire.passingScore(), 3, 10, false, questionnaire.questions());
		when(trainings.requireQuestionnaire(versionId, questionnaireId)).thenReturn(threeAttempts);
		assertThat(service().availability(assignmentId, questionnaireId).available()).isTrue();
		assertThat(service().availability(assignmentId, questionnaireId).nextAttemptAt()).isNull();
	}

	@Test
	void idempotentReplayDoesNotAllocateAnotherAttempt() {
		ExecutionQuestionnaireDetail questionnaire = questionnaire(1, 3, 0, false);
		AssessmentAttempt previous = attempt(1, now.minusSeconds(60), AssessmentResult.APPROVED);
		AssessmentAttemptRequest request = request(answer(questionnaire.questions().get(0), 0));
		when(assignments.requireOwner(assignmentId, false)).thenReturn(assignment);
		when(assignments.view(assignmentId)).thenReturn(assignment);
		when(trainings.requireQuestionnaire(versionId, questionnaireId)).thenReturn(questionnaire);
		when(attempts.findByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndIdempotencyKey(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId, "same")).thenReturn(Optional.of(previous));
		org.springframework.test.util.ReflectionTestUtils.setField(previous, "requestHash", hashOf(service(), request));
		service().submit(assignmentId, questionnaireId, request, "same");
		verify(assignments, never()).lockForAssessment(any());
		verify(attempts, never()).saveAndFlush(any());
	}

	@Test
	void completesOnlyAfterEveryActiveQuestionnaireAndUsesAveragePassedScore() {
		ExecutionQuestionnaireDetail questionnaire = questionnaire(1, 3, 0, false);
		stubSubmission(questionnaire);
		UUID otherQuestionnaire = UUID.randomUUID();
		java.util.concurrent.atomic.AtomicBoolean saved = new java.util.concurrent.atomic.AtomicBoolean();
		when(trainings.content(versionId)).thenReturn(new ExecutionContent(versionId, List.of(
				module(questionnaireId, 1), module(otherQuestionnaire, 2))));
		when(attempts.existsByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndResult(any(), any(), any(), any()))
				.thenAnswer(invocation -> saved.get());
		when(attempts.saveAndFlush(any())).thenAnswer(invocation -> { saved.set(true); return invocation.getArgument(0); });
		when(attempts.findAllByOrganizationIdAndAssignmentIdAndQuestionnaireIdInAndResult(any(), any(), any(), any()))
				.thenReturn(List.of(attemptFor(questionnaireId, new BigDecimal("80.00")),
						attemptFor(otherQuestionnaire, new BigDecimal("100.00"))));
		when(assignments.assessmentResult(assignmentId, true, true)).thenReturn(assignment(AssignmentStatus.APPROVED));
		when(assignments.complete(assignmentId, "ALL_ASSESSMENTS_APPROVED")).thenReturn(assignment(AssignmentStatus.COMPLETED));
		when(completions.automatic(any(), any())).thenReturn(new CompletionResponse(UUID.randomUUID(), employeeId,
				trainingId, versionId, assignmentId, now.atZone(ZoneOffset.UTC).toLocalDate(), now, CompletionForm.AUTOMATIC,
				new BigDecimal("90.00"), ValidityType.MONTHS, 12, now.atZone(ZoneOffset.UTC).toLocalDate().plusMonths(12),
				null, null, null));
		var result = service().submit(assignmentId, questionnaireId,
				request(answer(questionnaire.questions().get(0), 0)), "all-pass");
		assertThat(result.assignmentStatus()).isEqualTo(AssignmentStatus.COMPLETED);
		verify(assignments).lockForAssessment(assignmentId);
		verify(completions).automatic(any(), org.mockito.ArgumentMatchers.eq(new BigDecimal("90.00")));
	}

	private void stubSubmission(ExecutionQuestionnaireDetail questionnaire) {
		when(assignments.requireOwner(assignmentId, false)).thenReturn(assignment);
		when(assignments.lockForAssessment(assignmentId)).thenReturn(assignment);
		when(trainings.requireQuestionnaire(versionId, questionnaireId)).thenReturn(questionnaire);
		when(progress.requiredContentReady(assignmentId, versionId)).thenReturn(true);
		when(attempts.findFirstByOrganizationIdAndAssignmentIdAndQuestionnaireIdOrderByAttemptNumberDesc(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId)).thenReturn(Optional.empty());
	}
	private AssessmentService service() { return new AssessmentService(attempts, answers, assignments, trainings, progress,
			completions, authorization, events, Clock.fixed(now, ZoneOffset.UTC)); }
	private ExecutionQuestionnaireDetail questionnaire(int questionCount, Integer maxAttempts, int retry, boolean shuffle) {
		List<ExecutionQuestion> questions = java.util.stream.IntStream.range(0, questionCount).mapToObj(index -> {
			UUID question = UUID.randomUUID();
			return new ExecutionQuestion(question, "Question " + index, index + 1, List.of(
					new ExecutionOption(UUID.randomUUID(), "Correct", 1, true),
					new ExecutionOption(UUID.randomUUID(), "Wrong", 2, false)));
		}).toList();
		return new ExecutionQuestionnaireDetail(questionnaireId, versionId, "Assessment", new BigDecimal("70.00"),
				maxAttempts, retry, shuffle, questions);
	}
	private AssessmentAttemptRequest request(AssessmentAttemptRequest.Answer... values) { return new AssessmentAttemptRequest(List.of(values)); }
	private AssessmentAttemptRequest.Answer answer(ExecutionQuestion question, int option) {
		return new AssessmentAttemptRequest.Answer(question.id(), question.options().get(option).id());
	}
	private AssignmentExecutionPort.ExecutionAssignment assignment(AssignmentStatus status) {
		return new AssignmentExecutionPort.ExecutionAssignment(assignmentId, DEFAULT_ORGANIZATION_ID, employeeId, trainingId, versionId, status);
	}
	private AssessmentAttempt attempt(int number, Instant submittedAt, AssessmentResult result) {
		return new AssessmentAttempt(DEFAULT_ORGANIZATION_ID, assignmentId, employeeId, trainingId, versionId, questionnaireId,
				number, submittedAt, result == AssessmentResult.APPROVED ? new BigDecimal("70.00") : new BigDecimal("50.00"),
				new BigDecimal("70.00"), result, "key", "hash");
	}
	private AssessmentAttempt attemptFor(UUID questionnaire, BigDecimal score) {
		return new AssessmentAttempt(DEFAULT_ORGANIZATION_ID, assignmentId, employeeId, trainingId, versionId, questionnaire,
				1, now, score, new BigDecimal("70.00"), AssessmentResult.APPROVED, UUID.randomUUID().toString(), "hash");
	}
	private ExecutionModule module(UUID questionnaire, int order) {
		return new ExecutionModule(UUID.randomUUID(), "Module", null, order, List.of(),
				new ExecutionQuestionnaire(questionnaire, "Assessment", order));
	}
	private String hashOf(AssessmentService service, AssessmentAttemptRequest request) {
		return (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "hash", request);
	}
}
