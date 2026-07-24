package dev.igorbarbosa.worktrainingsystem.trainings.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ChangeTrainingStatusRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.CreateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ModuleRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ModuleResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.QuestionnaireResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.UpdateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.VideoResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.AnswerOption;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Question;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Questionnaire;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Training;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingModule;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Video;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.AnswerOptionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.QuestionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.QuestionnaireRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingModuleRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingVersionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.VideoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingCatalogService {

	private final TrainingRepository trainingRepository;
	private final TrainingVersionRepository versionRepository;
	private final TrainingModuleRepository moduleRepository;
	private final VideoRepository videoRepository;
	private final QuestionnaireRepository questionnaireRepository;
	private final QuestionRepository questionRepository;
	private final AnswerOptionRepository answerOptionRepository;

	public TrainingCatalogService(
			TrainingRepository trainingRepository,
			TrainingVersionRepository versionRepository,
			TrainingModuleRepository moduleRepository,
			VideoRepository videoRepository,
			QuestionnaireRepository questionnaireRepository,
			QuestionRepository questionRepository,
			AnswerOptionRepository answerOptionRepository) {
		this.trainingRepository = trainingRepository;
		this.versionRepository = versionRepository;
		this.moduleRepository = moduleRepository;
		this.videoRepository = videoRepository;
		this.questionnaireRepository = questionnaireRepository;
		this.questionRepository = questionRepository;
		this.answerOptionRepository = answerOptionRepository;
	}

	@Transactional
	public TrainingResponse create(CreateTrainingRequest request) {
		String code = normalizeCode(request.code());
		if (trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, code)) {
			throw new ResourceConflictException("TRAINING_CODE_ALREADY_EXISTS",
					"Já existe um treinamento com o código informado.");
		}
		validateVersionValues(request.initialVersion().validityType(), request.initialVersion().validityValue(),
				request.initialVersion().passingScore());
		Training training = new Training(DEFAULT_ORGANIZATION_ID, request.name().trim(), code, trim(request.description()),
				trim(request.category()), request.regulatoryStandard(), request.status());
		try {
			training = trainingRepository.saveAndFlush(training);
			CreateTrainingRequest.InitialVersion initial = request.initialVersion();
			versionRepository.saveAndFlush(new TrainingVersion(training.getId(), 1, initial.workloadMinutes(),
					initial.validityType(), initial.validityValue(), initial.passingScore(), initial.maxAttempts(),
					initial.retryIntervalMinutes()));
			return TrainingResponse.from(training);
		} catch (DataIntegrityViolationException exception) {
			throw new ResourceConflictException("TRAINING_CODE_ALREADY_EXISTS",
					"Já existe um treinamento com o código informado.");
		}
	}

	@Transactional(readOnly = true)
	public Page<TrainingResponse> list(String search, RegistrationStatus status, Pageable pageable) {
		Specification<Training> specification = (root, query, builder) ->
				builder.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
		String normalizedSearch = search == null || search.isBlank()
				? null : search.trim().toLowerCase(Locale.ROOT);
		if (normalizedSearch != null) {
			specification = specification.and((root, query, builder) -> builder.or(
					builder.like(builder.lower(root.get("name")), "%" + normalizedSearch + "%"),
					builder.like(builder.lower(root.get("code")), "%" + normalizedSearch + "%")));
		}
		if (status != null) {
			specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
		}
		return trainingRepository.findAll(specification, pageable).map(TrainingResponse::from);
	}

	@Transactional(readOnly = true)
	public TrainingResponse getTraining(UUID trainingId) {
		return TrainingResponse.from(findTraining(trainingId));
	}

	@Transactional
	public TrainingResponse updateTraining(UUID trainingId, UpdateTrainingRequest request) {
		Training training = findTraining(trainingId);
		String code = normalizeCode(request.code());
		if (!training.getCode().equalsIgnoreCase(code)
				&& trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, code)) {
			throw new ResourceConflictException("TRAINING_CODE_ALREADY_EXISTS",
					"Já existe um treinamento com o código informado.");
		}
		training.update(request.name().trim(), code, trim(request.description()), trim(request.category()),
				request.regulatoryStandard());
		return TrainingResponse.from(training);
	}

	@Transactional
	public TrainingResponse changeTrainingStatus(UUID trainingId, ChangeTrainingStatusRequest request) {
		Training training = findTraining(trainingId);
		training.changeStatus(request.status());
		return TrainingResponse.from(training);
	}

	@Transactional(readOnly = true)
	public List<TrainingVersionResponse> listVersions(UUID trainingId) {
		findTraining(trainingId);
		return versionRepository.findAllByTrainingIdOrderByVersionNumberDesc(trainingId).stream()
				.map(TrainingVersionResponse::from).toList();
	}

	@Transactional
	public TrainingVersionResponse createVersion(UUID trainingId, TrainingVersionRequest request) {
		findTraining(trainingId);
		validateVersionValues(request.validityType(), request.validityValue(), request.passingScore());
		int versionNumber = versionRepository.countByTrainingId(trainingId) + 1;
		return TrainingVersionResponse.from(versionRepository.saveAndFlush(new TrainingVersion(trainingId, versionNumber,
				request.workloadMinutes(), request.validityType(), request.validityValue(), request.passingScore(),
				request.maxAttempts(), request.retryIntervalMinutes())));
	}

	@Transactional(readOnly = true)
	public TrainingVersionResponse getVersion(UUID versionId) {
		return TrainingVersionResponse.from(findVersion(versionId));
	}

	@Transactional
	public TrainingVersionResponse updateVersion(UUID versionId, TrainingVersionRequest request) {
		TrainingVersion version = draftVersion(versionId);
		validateVersionValues(request.validityType(), request.validityValue(), request.passingScore());
		version.update(request.workloadMinutes(), request.validityType(), request.validityValue(), request.passingScore(),
				request.maxAttempts(), request.retryIntervalMinutes());
		return TrainingVersionResponse.from(version);
	}

	@Transactional
	public TrainingVersionResponse publishVersion(UUID versionId) {
		TrainingVersion version = draftVersion(versionId);
		Training training = findTraining(version.getTrainingId());
		if (training.getStatus() != RegistrationStatus.ACTIVE) {
			throw rule("TRAINING_INACTIVE", "Treinamentos inativos não podem ter versões publicadas.");
		}
		validateVersionValues(version.getValidityType(), version.getValidityValue(), version.getPassingScore());
		validatePublicationContent(version);
		versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(version.getTrainingId(),
				TrainingVersionStatus.PUBLISHED).ifPresent(TrainingVersion::archive);
		version.publish(Instant.now());
		return TrainingVersionResponse.from(versionRepository.saveAndFlush(version));
	}

	@Transactional
	public TrainingVersionResponse archiveVersion(UUID versionId) {
		TrainingVersion version = findVersion(versionId);
		if (version.getStatus() == TrainingVersionStatus.ARCHIVED) {
			return TrainingVersionResponse.from(version);
		}
		if (version.getStatus() != TrainingVersionStatus.PUBLISHED) {
			throw rule("INVALID_STATE_TRANSITION", "Somente versões publicadas podem ser arquivadas.");
		}
		version.archive();
		return TrainingVersionResponse.from(version);
	}

	@Transactional
	public ModuleResponse createModule(UUID versionId, ModuleRequest request) {
		TrainingVersion version = draftVersion(versionId);
		return ModuleResponse.from(moduleRepository.saveAndFlush(new TrainingModule(version.getId(), request.title().trim(),
				trim(request.description()), request.order(), request.status())));
	}

	@Transactional(readOnly = true)
	public List<ModuleResponse> listModules(UUID versionId) {
		findVersion(versionId);
		return moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId).stream()
				.map(ModuleResponse::from).toList();
	}

	@Transactional
	public ModuleResponse updateModule(UUID moduleId, ModuleRequest request) {
		TrainingModule module = findModule(moduleId);
		draftVersion(module.getTrainingVersionId());
		module.update(request.title().trim(), trim(request.description()), request.order());
		module.changeStatus(request.status());
		return ModuleResponse.from(module);
	}

	@Transactional(readOnly = true)
	public ModuleResponse getModule(UUID moduleId) {
		return ModuleResponse.from(findModule(moduleId));
	}

	@Transactional
	public VideoResponse createVideo(UUID moduleId, VideoRequest request) {
		TrainingModule module = findModule(moduleId);
		draftVersion(module.getTrainingVersionId());
		return VideoResponse.from(videoRepository.saveAndFlush(new Video(moduleId, request.title().trim(),
				trim(request.description()), request.order(), request.durationSeconds(), request.storageObjectKey().trim(),
				request.required(), request.status())));
	}

	@Transactional(readOnly = true)
	public VideoResponse getVideo(UUID videoId) {
		return VideoResponse.from(findVideo(videoId));
	}

	@Transactional
	public VideoResponse updateVideo(UUID videoId, VideoRequest request) {
		Video video = findVideo(videoId);
		draftVersion(findModule(video.getModuleId()).getTrainingVersionId());
		video.update(request.title().trim(), trim(request.description()), request.order(), request.durationSeconds(),
				request.storageObjectKey().trim(), request.required());
		video.changeStatus(request.status());
		return VideoResponse.from(video);
	}

	@Transactional
	public QuestionnaireResponse createQuestionnaire(UUID moduleId, QuestionnaireRequest request) {
		TrainingModule module = findModule(moduleId);
		draftVersion(module.getTrainingVersionId());
		if (questionnaireRepository.findByModuleId(moduleId).isPresent()) {
			throw new ResourceConflictException("QUESTIONNAIRE_ALREADY_EXISTS",
					"O módulo já possui um questionário.");
		}
		return QuestionnaireResponse.from(questionnaireRepository.saveAndFlush(new Questionnaire(moduleId, request.title().trim(),
				request.passingScore(), request.maxAttempts(), request.retryIntervalMinutes(), request.shuffleQuestions(),
				request.status())));
	}

	@Transactional(readOnly = true)
	public QuestionnaireResponse getQuestionnaire(UUID questionnaireId) {
		return QuestionnaireResponse.from(findQuestionnaire(questionnaireId));
	}

	@Transactional
	public QuestionnaireResponse updateQuestionnaire(UUID questionnaireId, QuestionnaireRequest request) {
		Questionnaire questionnaire = findQuestionnaireInDraft(questionnaireId);
		questionnaire.update(request.title().trim(), request.passingScore(), request.maxAttempts(),
				request.retryIntervalMinutes(), request.shuffleQuestions());
		questionnaire.changeStatus(request.status());
		return QuestionnaireResponse.from(questionnaire);
	}

	@Transactional
	public QuestionResponse createQuestion(UUID questionnaireId, QuestionRequest request) {
		findQuestionnaireInDraft(questionnaireId);
		return QuestionResponse.from(questionRepository.saveAndFlush(new Question(questionnaireId, request.statement().trim(),
				request.order(), request.status())));
	}

	@Transactional
	public AnswerOptionResponse createAnswerOption(UUID questionId, AnswerOptionRequest request) {
		Question question = findQuestion(questionId);
		findQuestionnaireInDraft(question.getQuestionnaireId());
		if (request.correct() && answerOptionRepository.countByQuestionIdAndStatusAndCorrect(questionId,
				RegistrationStatus.ACTIVE, true) > 0) {
			throw rule("MULTIPLE_CORRECT_ANSWERS", "Cada questão deve possuir uma única alternativa correta.");
		}
		return AnswerOptionResponse.from(answerOptionRepository.saveAndFlush(new AnswerOption(questionId, request.text().trim(),
				request.correct(), request.order(), request.status())));
	}

	@Transactional(readOnly = true)
	public List<QuestionResponse> listQuestions(UUID questionnaireId) {
		findQuestionnaire(questionnaireId);
		return questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaireId).stream()
				.map(QuestionResponse::from).toList();
	}

	@Transactional
	public QuestionResponse updateQuestion(UUID questionId, QuestionRequest request) {
		Question question = findQuestion(questionId);
		findQuestionnaireInDraft(question.getQuestionnaireId());
		question.update(request.statement().trim(), request.order());
		question.changeStatus(request.status());
		return QuestionResponse.from(question);
	}

	@Transactional(readOnly = true)
	public QuestionResponse getQuestion(UUID questionId) {
		return QuestionResponse.from(findQuestion(questionId));
	}

	@Transactional(readOnly = true)
	public List<AnswerOptionResponse> listAnswerOptions(UUID questionId) {
		findQuestion(questionId);
		return answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(questionId).stream()
				.map(AnswerOptionResponse::from).toList();
	}

	@Transactional
	public AnswerOptionResponse updateAnswerOption(UUID optionId, AnswerOptionRequest request) {
		AnswerOption option = answerOptionRepository.findById(optionId)
				.orElseThrow(() -> new ResourceNotFoundException("A alternativa informada não existe."));
		findQuestionnaireInDraft(findQuestion(option.getQuestionId()).getQuestionnaireId());
		if (request.correct() && !option.isCorrect()
				&& answerOptionRepository.countByQuestionIdAndStatusAndCorrect(option.getQuestionId(),
						RegistrationStatus.ACTIVE, true) > 0) {
			throw rule("MULTIPLE_CORRECT_ANSWERS", "Cada questão deve possuir uma única alternativa correta.");
		}
		option.update(request.text().trim(), request.correct(), request.order());
		option.changeStatus(request.status());
		return AnswerOptionResponse.from(option);
	}

	@Transactional(readOnly = true)
	public AnswerOptionResponse getAnswerOption(UUID optionId) {
		return AnswerOptionResponse.from(answerOptionRepository.findById(optionId)
				.orElseThrow(() -> new ResourceNotFoundException("A alternativa informada não existe.")));
	}

	private void validatePublicationContent(TrainingVersion version) {
		List<TrainingModule> modules = moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(version.getId()).stream()
				.filter(module -> module.getStatus() == RegistrationStatus.ACTIVE).toList();
		if (modules.isEmpty()) {
			throw rule("VERSION_CONTENT_INVALID", "A versão deve possuir ao menos um módulo ativo.");
		}
		for (TrainingModule module : modules) {
			for (Video video : videoRepository.findAllByModuleIdOrderByDisplayOrder(module.getId())) {
				if (video.getStatus() == RegistrationStatus.ACTIVE
						&& (video.getStorageObjectKey().isBlank() || video.getDurationSeconds() <= 0)) {
					throw rule("VERSION_CONTENT_INVALID", "Todos os vídeos ativos devem possuir duração e arquivo válidos.");
				}
			}
			questionnaireRepository.findByModuleId(module.getId())
					.filter(questionnaire -> questionnaire.getStatus() == RegistrationStatus.ACTIVE)
					.ifPresent(questionnaire -> validateQuestionnaire(questionnaire));
		}
	}

	private void validateQuestionnaire(Questionnaire questionnaire) {
		List<Question> questions = questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaire.getId()).stream()
				.filter(question -> question.getStatus() == RegistrationStatus.ACTIVE).toList();
		if (questions.isEmpty()) {
			throw rule("VERSION_CONTENT_INVALID", "Cada questionário ativo deve possuir ao menos uma questão.");
		}
		for (Question question : questions) {
			List<AnswerOption> options = answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(question.getId()).stream()
					.filter(option -> option.getStatus() == RegistrationStatus.ACTIVE).toList();
			long correct = options.stream().filter(AnswerOption::isCorrect).count();
			if (options.size() < 2 || correct != 1) {
				throw rule("VERSION_CONTENT_INVALID",
						"Cada questão deve possuir ao menos duas alternativas e exatamente uma correta.");
			}
		}
	}

	private void validateVersionValues(ValidityType validityType, Integer validityValue, BigDecimal passingScore) {
		if (validityType == ValidityType.INDEFINITE && validityValue != null
				|| validityType != ValidityType.INDEFINITE && (validityValue == null || validityValue <= 0)) {
			throw rule("INVALID_VALIDITY", "O valor da validade não corresponde ao tipo informado.");
		}
		if (passingScore == null || passingScore.compareTo(BigDecimal.ZERO) < 0
				|| passingScore.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw rule("INVALID_PASSING_SCORE", "A nota mínima deve estar entre 0 e 100.");
		}
	}

	private Training findTraining(UUID id) {
		return trainingRepository.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O treinamento informado não existe."));
	}

	private TrainingVersion findVersion(UUID id) {
		return versionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("A versão informada não existe."));
	}

	private TrainingVersion draftVersion(UUID id) {
		TrainingVersion version = findVersion(id);
		if (version.getStatus() != TrainingVersionStatus.DRAFT) {
			throw rule("PUBLISHED_CONTENT_IMMUTABLE", "O conteúdo publicado não pode ser alterado.");
		}
		return version;
	}

	private TrainingModule findModule(UUID id) {
		return moduleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("O módulo informado não existe."));
	}

	private Video findVideo(UUID id) {
		return videoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("O vídeo informado não existe."));
	}

	private Questionnaire findQuestionnaire(UUID id) {
		return questionnaireRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("O questionário informado não existe."));
	}

	private Questionnaire findQuestionnaireInDraft(UUID id) {
		Questionnaire questionnaire = findQuestionnaire(id);
		draftVersion(findModule(questionnaire.getModuleId()).getTrainingVersionId());
		return questionnaire;
	}

	private Question findQuestion(UUID id) {
		return questionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("A questão informada não existe."));
	}

	private String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private String trim(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private BusinessRuleViolationException rule(String code, String message) {
		return new BusinessRuleViolationException(code, message);
	}
}
