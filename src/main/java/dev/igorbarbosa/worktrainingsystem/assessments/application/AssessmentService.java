package dev.igorbarbosa.worktrainingsystem.assessments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptDetailResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptRequest;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAttemptSummaryResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.AssessmentAvailabilityResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.api.QuestionnaireDeliveryResponse;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentAttempt;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AssessmentResult;
import dev.igorbarbosa.worktrainingsystem.assessments.domain.AttemptAnswer;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AssessmentAttemptRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.AttemptAnswerRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentExecutionPort.ExecutionAssignment;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.progress.application.ContentProgressPort;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionOption;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionQuestion;
import dev.igorbarbosa.worktrainingsystem.trainings.application.TrainingExecutionCatalog.ExecutionQuestionnaireDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
	private final AssessmentAttemptRepository attempts;
	private final AttemptAnswerRepository answers;
	private final AssignmentExecutionPort assignments;
	private final TrainingExecutionCatalog trainings;
	private final ContentProgressPort progress;
	private final CompletionService completions;
	private final AuthorizationService authorization;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	public AssessmentService(AssessmentAttemptRepository attempts, AttemptAnswerRepository answers,
			AssignmentExecutionPort assignments, TrainingExecutionCatalog trainings, ContentProgressPort progress,
			CompletionService completions, AuthorizationService authorization, ApplicationEventPublisher events, Clock clock) {
		this.attempts = attempts; this.answers = answers; this.assignments = assignments; this.trainings = trainings;
		this.progress = progress; this.completions = completions; this.authorization = authorization;
		this.events = events; this.clock = clock;
	}

	@Transactional
	public QuestionnaireDeliveryResponse questionnaire(UUID assignmentId, UUID questionnaireId) {
		ExecutionAssignment assignment = assignments.requireOwner(assignmentId, false);
		ExecutionQuestionnaireDetail questionnaire = requireReadyQuestionnaire(assignment, questionnaireId);
		assignment = assessmentReadyState(assignment);
		int nextAttempt = last(assignmentId, questionnaireId).map(item -> item.getAttemptNumber() + 1).orElse(1);
		List<ExecutionQuestion> ordered = orderedQuestions(questionnaire, assignmentId, nextAttempt);
		return new QuestionnaireDeliveryResponse(questionnaire.id(), questionnaire.title(), questionnaire.shuffleQuestions(),
				ordered.stream().map(question -> new QuestionnaireDeliveryResponse.Question(question.id(), question.statement(),
						question.options().stream().map(option -> new QuestionnaireDeliveryResponse.Option(option.id(), option.text())).toList()))
						.toList());
	}

	@Transactional
	public AssessmentAvailabilityResponse availability(UUID assignmentId, UUID questionnaireId) {
		ExecutionAssignment assignment = assignments.requireOwner(assignmentId, false);
		ExecutionQuestionnaireDetail questionnaire = requireReadyQuestionnaire(assignment, questionnaireId);
		assessmentReadyState(assignment);
		return availability(assignmentId, questionnaire);
	}

	@Transactional
	public AssessmentAttemptResponse submit(UUID assignmentId, UUID questionnaireId,
			AssessmentAttemptRequest request, String idempotencyKey) {
		String key = requiredKey(idempotencyKey); String requestHash = hash(request);
		ExecutionAssignment visible = assignments.requireOwner(assignmentId, false);
		ExecutionQuestionnaireDetail questionnaire = trainings.requireQuestionnaire(visible.trainingVersionId(), questionnaireId);
		var duplicate = attempts.findByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndIdempotencyKey(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId, key);
		if (duplicate.isPresent()) return replay(duplicate.get(), requestHash, assignments.view(assignmentId));
		if (!progress.requiredContentReady(assignmentId, visible.trainingVersionId()))
			throw conflict("REQUIRED_CONTENT_NOT_READY", "Conclua os vídeos obrigatórios antes da avaliação.");
		visible = assessmentReadyState(visible);

		ExecutionAssignment assignment = assignments.lockForAssessment(assignmentId);
		duplicate = attempts.findByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndIdempotencyKey(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId, key);
		if (duplicate.isPresent()) return replay(duplicate.get(), requestHash, assignment);
		if (!progress.requiredContentReady(assignmentId, assignment.trainingVersionId()))
			throw conflict("REQUIRED_CONTENT_NOT_READY", "Conclua os vídeos obrigatórios antes da avaliação.");
		AssessmentAvailabilityResponse availability = availability(assignmentId, questionnaire);
		if (!availability.available()) {
			if (availability.attemptsRemaining() != null && availability.attemptsRemaining() == 0)
				throw conflict("MAX_ATTEMPTS_REACHED", "O limite de tentativas foi atingido.");
			throw conflict("RETRY_INTERVAL_NOT_REACHED", "A próxima tentativa ainda não está disponível.");
		}
		Map<UUID, AssessmentAttemptRequest.Answer> submitted = validateCompleteAnswers(questionnaire, request);
		Map<UUID, ExecutionOption> selected = selectedOptions(questionnaire, submitted);
		long correct = selected.values().stream().filter(ExecutionOption::correct).count();
		BigDecimal score = score(correct, questionnaire.questions().size());
		AssessmentResult result = score.compareTo(questionnaire.passingScore()) >= 0
				? AssessmentResult.APPROVED : AssessmentResult.FAILED;
		Instant now = clock.instant(); int attemptNumber = availability.attemptsUsed() + 1;
		AssessmentAttempt attempt = attempts.saveAndFlush(new AssessmentAttempt(DEFAULT_ORGANIZATION_ID, assignmentId,
				assignment.employeeId(), assignment.trainingId(), assignment.trainingVersionId(), questionnaireId,
				attemptNumber, now, score, questionnaire.passingScore(), result, key, requestHash));
		for (ExecutionQuestion question : questionnaire.questions()) {
			ExecutionOption option = selected.get(question.id());
			answers.save(new AttemptAnswer(DEFAULT_ORGANIZATION_ID, attempt.getId(), questionnaireId, question.id(),
					option.id(), question.statement(), option.text(), option.correct(), now));
		}
		answers.flush();

		List<UUID> requiredQuestionnaires = trainings.content(assignment.trainingVersionId()).modules().stream()
				.filter(module -> module.questionnaire() != null).map(module -> module.questionnaire().id()).toList();
		boolean allPassed = result == AssessmentResult.APPROVED && requiredQuestionnaires.stream().allMatch(id ->
				attempts.existsByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndResult(DEFAULT_ORGANIZATION_ID,
						assignmentId, id, AssessmentResult.APPROVED));
		assignment = assignments.assessmentResult(assignmentId, result == AssessmentResult.APPROVED, allPassed);
		Instant completedAt = null;
		if (allPassed) {
			BigDecimal finalScore = aggregateScore(assignmentId, requiredQuestionnaires);
			var completion = completions.automatic(assignment, finalScore);
			assignment = assignments.complete(assignmentId, "ALL_ASSESSMENTS_APPROVED");
			completedAt = completion.completedAt();
		} else if (result == AssessmentResult.FAILED) {
			events.publishEvent(TrainingOutcomeEvent.failed(assignment.employeeId(), assignment.trainingId(), attempt.getId()));
		}
		Instant nextAttemptAt = result == AssessmentResult.FAILED && canRetry(attemptNumber, questionnaire.maxAttempts())
				? now.plus(questionnaire.retryIntervalMinutes(), ChronoUnit.MINUTES) : null;
		return new AssessmentAttemptResponse(attempt.getId(), attemptNumber, score, questionnaire.passingScore(),
				result, assignment.status(), completedAt, nextAttemptAt);
	}

	@Transactional(readOnly = true)
	public Page<AssessmentAttemptSummaryResponse> history(UUID assignmentId, Pageable pageable) {
		return attempts.findAll(visible().and(equal("assignmentId", assignmentId)), pageable).map(this::summary);
	}

	@Transactional(readOnly = true)
	public AssessmentAttemptDetailResponse detail(UUID attemptId) {
		AssessmentAttempt attempt = attempts.findOne(visible().and(equal("id", attemptId)))
				.orElseThrow(() -> inaccessibleOrMissing(attemptId));
		List<AssessmentAttemptDetailResponse.Answer> detailAnswers = answers.findAllByOrganizationIdAndAttemptId(
				DEFAULT_ORGANIZATION_ID, attemptId).stream()
				.map(answer -> new AssessmentAttemptDetailResponse.Answer(answer.getQuestionId(),
						answer.getQuestionStatementSnapshot(), answer.getSelectedOptionId(),
						answer.getSelectedOptionTextSnapshot(), answer.isCorrect())).toList();
		return new AssessmentAttemptDetailResponse(attempt.getId(), attempt.getAssignmentId(), attempt.getEmployeeId(),
				attempt.getTrainingId(), attempt.getTrainingVersionId(), attempt.getQuestionnaireId(),
				attempt.getAttemptNumber(), attempt.getSubmittedAt(), attempt.getScore(), attempt.getPassingScore(),
				attempt.getResult(), detailAnswers);
	}

	public static BigDecimal score(long correct, int total) {
		if (total <= 0 || correct < 0 || correct > total) throw new IllegalArgumentException("score operands");
		return BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
	}

	List<ExecutionQuestion> orderedQuestions(ExecutionQuestionnaireDetail questionnaire, UUID assignmentId, int attemptNumber) {
		List<ExecutionQuestion> result = new ArrayList<>(questionnaire.questions());
		if (questionnaire.shuffleQuestions()) {
			UUID seed = UUID.nameUUIDFromBytes((assignmentId + ":" + questionnaire.id() + ":" + attemptNumber)
					.getBytes(StandardCharsets.UTF_8));
			Collections.shuffle(result, new Random(seed.getMostSignificantBits() ^ seed.getLeastSignificantBits()));
		}
		return List.copyOf(result);
	}

	private ExecutionQuestionnaireDetail requireReadyQuestionnaire(ExecutionAssignment assignment, UUID questionnaireId) {
		ExecutionQuestionnaireDetail questionnaire = trainings.requireQuestionnaire(assignment.trainingVersionId(), questionnaireId);
		if (!progress.requiredContentReady(assignment.id(), assignment.trainingVersionId()))
			throw conflict("REQUIRED_CONTENT_NOT_READY", "Conclua os vídeos obrigatórios antes da avaliação.");
		return questionnaire;
	}
	private ExecutionAssignment assessmentReadyState(ExecutionAssignment assignment) {
		if (assignment.status() == dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus.IN_PROGRESS)
			assignment = assignments.contentReady(assignment.id(), true);
		if (assignment.status() != dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus.AWAITING_ASSESSMENT
				&& assignment.status() != dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus.FAILED)
			throw conflict("ASSESSMENT_NOT_AVAILABLE", "A avaliação não está disponível neste estado.");
		return assignment;
	}
	private AssessmentAvailabilityResponse availability(UUID assignmentId, ExecutionQuestionnaireDetail questionnaire) {
		AssessmentAttempt last = last(assignmentId, questionnaire.id()).orElse(null);
		int used = last == null ? 0 : last.getAttemptNumber();
		Integer remaining = questionnaire.maxAttempts() == null ? null : Math.max(0, questionnaire.maxAttempts() - used);
		boolean passed = attempts.existsByOrganizationIdAndAssignmentIdAndQuestionnaireIdAndResult(DEFAULT_ORGANIZATION_ID,
				assignmentId, questionnaire.id(), AssessmentResult.APPROVED);
		Instant next = last == null || passed ? null : last.getSubmittedAt().plus(questionnaire.retryIntervalMinutes(), ChronoUnit.MINUTES);
		boolean intervalReached = next == null || !clock.instant().isBefore(next);
		boolean available = !passed && (remaining == null || remaining > 0) && intervalReached;
		return new AssessmentAvailabilityResponse(assignmentId, questionnaire.id(), questionnaire.passingScore(), used,
				questionnaire.maxAttempts(), remaining, available,
				available || remaining != null && remaining == 0 || passed ? null : next);
	}
	private java.util.Optional<AssessmentAttempt> last(UUID assignmentId, UUID questionnaireId) {
		return attempts.findFirstByOrganizationIdAndAssignmentIdAndQuestionnaireIdOrderByAttemptNumberDesc(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireId);
	}
	private Map<UUID, AssessmentAttemptRequest.Answer> validateCompleteAnswers(ExecutionQuestionnaireDetail questionnaire,
			AssessmentAttemptRequest request) {
		Map<UUID, AssessmentAttemptRequest.Answer> submitted = new HashMap<>();
		for (var answer : request.answers()) if (submitted.put(answer.questionId(), answer) != null)
			throw rule("DUPLICATE_QUESTION_ANSWER", "Cada questão deve ser respondida uma única vez.");
		Set<UUID> expected = questionnaire.questions().stream().map(ExecutionQuestion::id).collect(java.util.stream.Collectors.toSet());
		if (!submitted.keySet().equals(expected))
			throw rule("INCOMPLETE_ANSWER_SET", "Informe exatamente uma resposta para cada questão ativa.");
		return submitted;
	}
	private Map<UUID, ExecutionOption> selectedOptions(ExecutionQuestionnaireDetail questionnaire,
			Map<UUID, AssessmentAttemptRequest.Answer> submitted) {
		Map<UUID, ExecutionOption> result = new HashMap<>();
		for (ExecutionQuestion question : questionnaire.questions()) {
			UUID selectedId = submitted.get(question.id()).answerOptionId();
			ExecutionOption selected = question.options().stream().filter(option -> option.id().equals(selectedId)).findFirst()
					.orElseThrow(() -> rule("ANSWER_OPTION_NOT_IN_QUESTION", "A alternativa não pertence à questão ou está inativa."));
			result.put(question.id(), selected);
		}
		return result;
	}
	private BigDecimal aggregateScore(UUID assignmentId, List<UUID> questionnaireIds) {
		List<AssessmentAttempt> passed = attempts.findAllByOrganizationIdAndAssignmentIdAndQuestionnaireIdInAndResult(
				DEFAULT_ORGANIZATION_ID, assignmentId, questionnaireIds, AssessmentResult.APPROVED);
		Map<UUID, AssessmentAttempt> latest = new HashMap<>();
		passed.forEach(item -> latest.merge(item.getQuestionnaireId(), item,
				(left, right) -> left.getSubmittedAt().isAfter(right.getSubmittedAt()) ? left : right));
		return latest.values().stream().map(AssessmentAttempt::getScore).reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(questionnaireIds.size()), 2, RoundingMode.HALF_UP);
	}
	private AssessmentAttemptResponse replay(AssessmentAttempt attempt, String requestHash, ExecutionAssignment assignment) {
		if (!attempt.getRequestHash().equals(requestHash))
			throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de idempotência foi reutilizada com outros dados.");
		ExecutionQuestionnaireDetail questionnaire = attempt.getResult() == AssessmentResult.FAILED
				? trainings.requireQuestionnaire(attempt.getTrainingVersionId(), attempt.getQuestionnaireId()) : null;
		Instant next = questionnaire != null && canRetry(attempt.getAttemptNumber(), questionnaire.maxAttempts())
				? attempt.getSubmittedAt().plus(questionnaire.retryIntervalMinutes(), ChronoUnit.MINUTES) : null;
		return new AssessmentAttemptResponse(attempt.getId(), attempt.getAttemptNumber(), attempt.getScore(),
				attempt.getPassingScore(), attempt.getResult(), assignment.status(),
				completions.automaticCompletedAt(assignment.id()), next);
	}
	private AssessmentAttemptSummaryResponse summary(AssessmentAttempt item) {
		return new AssessmentAttemptSummaryResponse(item.getId(), item.getAssignmentId(), item.getQuestionnaireId(),
				item.getAttemptNumber(), item.getSubmittedAt(), item.getScore(), item.getPassingScore(), item.getResult());
	}
	private Specification<AssessmentAttempt> visible() {
		var scope = authorization.currentScope(); Specification<AssessmentAttempt> result = equal("organizationId", DEFAULT_ORGANIZATION_ID);
		if (scope.admin()) return result;
		if (scope.employee()) return result.and(equal("employeeId", scope.ownEmployeeId()));
		if (!scope.manager() || !scope.hasGrants()) return result.and((root, query, cb) -> cb.disjunction());
		Set<UUID> employeeIds = authorization.scopeReferences(scope).employeeIds();
		return employeeIds.isEmpty() ? result.and((root, query, cb) -> cb.disjunction())
				: result.and((root, query, cb) -> root.get("employeeId").in(employeeIds));
	}
	private Specification<AssessmentAttempt> equal(String property, Object value) {
		return (root, query, cb) -> cb.equal(root.get(property), value);
	}
	private RuntimeException inaccessibleOrMissing(UUID id) {
		return attempts.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent()
				? new AccessDeniedException("A tentativa está fora do escopo autorizado.")
				: new ResourceNotFoundException("A tentativa informada não existe.");
	}
	private String requiredKey(String value) {
		if (value == null || value.isBlank()) return UUID.randomUUID().toString();
		if (value.trim().length() > 200) throw rule("IDEMPOTENCY_KEY_INVALID", "A chave de idempotência é muito longa.");
		return value.trim();
	}
	private String hash(AssessmentAttemptRequest request) {
		String canonical = request.answers().stream().sorted(Comparator.comparing(answer -> answer.questionId().toString()))
				.map(answer -> answer.questionId() + ":" + answer.answerOptionId()).reduce((a, b) -> a + "|" + b).orElse("");
		try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
		catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
	}
	private boolean canRetry(int attemptNumber, Integer maxAttempts) { return maxAttempts == null || attemptNumber < maxAttempts; }
	private BusinessRuleViolationException rule(String code, String message) { return new BusinessRuleViolationException(code, message); }
	private ResourceConflictException conflict(String code, String message) { return new ResourceConflictException(code, message); }
}
