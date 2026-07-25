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
import dev.igorbarbosa.worktrainingsystem.trainings.api.ContentSummaryResponse;
import dev.igorbarbosa.worktrainingsystem.trainings.api.OrderRequest;
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
import dev.igorbarbosa.worktrainingsystem.files.application.UploadedFileCatalog;
import dev.igorbarbosa.worktrainingsystem.files.application.UploadProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingCatalogService implements TrainingCatalog, TrainingExecutionCatalog {
	private static final ObjectMapper JSON = new ObjectMapper();

	private final TrainingRepository trainingRepository;
	private final TrainingVersionRepository versionRepository;
	private final TrainingModuleRepository moduleRepository;
	private final VideoRepository videoRepository;
	private final QuestionnaireRepository questionnaireRepository;
	private final QuestionRepository questionRepository;
	private final AnswerOptionRepository answerOptionRepository;
	private final UploadedFileCatalog uploadedFiles;
	private final UploadProperties uploadProperties;

	public TrainingCatalogService(
			TrainingRepository trainingRepository,
			TrainingVersionRepository versionRepository,
			TrainingModuleRepository moduleRepository,
			VideoRepository videoRepository,
			QuestionnaireRepository questionnaireRepository,
			QuestionRepository questionRepository,
			AnswerOptionRepository answerOptionRepository,
			UploadedFileCatalog uploadedFiles,
			UploadProperties uploadProperties) {
		this.trainingRepository = trainingRepository;
		this.versionRepository = versionRepository;
		this.moduleRepository = moduleRepository;
		this.videoRepository = videoRepository;
		this.questionnaireRepository = questionnaireRepository;
		this.questionRepository = questionRepository;
		this.answerOptionRepository = answerOptionRepository;
		this.uploadedFiles = uploadedFiles;
		this.uploadProperties = uploadProperties;
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
			versionRepository.saveAndFlush(new TrainingVersion(training, 1, initial.workloadMinutes(),
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
		Training training = trainingRepository.findForUpdateByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O treinamento informado não existe."));
		String code = normalizeCode(request.code());
		if (!training.getCode().equalsIgnoreCase(code)
				&& trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, code)) {
			throw new ResourceConflictException("TRAINING_CODE_ALREADY_EXISTS",
					"Já existe um treinamento com o código informado.");
		}
		training.update(request.name().trim(), code, trim(request.description()), trim(request.category()),
				request.regulatoryStandard());
		var draft = versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberAsc(trainingId, TrainingVersionStatus.DRAFT);
		if (draft.isPresent()) draft.get().refreshDraftSnapshot(training);
		else versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(trainingId, TrainingVersionStatus.PUBLISHED)
				.ifPresent(published -> duplicateVersionInternal(published, training));
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
		Training training = trainingRepository.findForUpdateByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O treinamento informado não existe."));
		validateVersionValues(request.validityType(), request.validityValue(), request.passingScore());
		int versionNumber = versionRepository.findMaximumVersionNumber(trainingId) + 1;
		return TrainingVersionResponse.from(versionRepository.saveAndFlush(new TrainingVersion(training, versionNumber,
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
		version.refreshDraftSnapshot(training);
		version.capturePublicationSnapshot(training, publicationSnapshot(version));
		versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(version.getTrainingId(),
				TrainingVersionStatus.PUBLISHED).ifPresent(TrainingVersion::archive);
		versionRepository.flush();
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
		var file = requireVideoFile(request.fileId(), request.storageObjectKey());
		return VideoResponse.from(videoRepository.saveAndFlush(new Video(moduleId, request.title().trim(),
				trim(request.description()), request.order(), request.durationSeconds(), file.id(), file.objectKey(),
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
		var file = requireVideoFile(request.fileId(), request.storageObjectKey());
		video.update(request.title().trim(), trim(request.description()), request.order(), request.durationSeconds(),
				file.id(), file.objectKey(), request.required());
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
		validatePassingScore(request.passingScore());
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
		validatePassingScore(request.passingScore());
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
				RegistrationStatus.ACTIVE, true) > 0 && request.status() == RegistrationStatus.ACTIVE) {
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
		if (request.correct() && request.status() == RegistrationStatus.ACTIVE
				&& answerOptionRepository.countByQuestionIdAndStatusAndCorrectAndIdNot(option.getQuestionId(),
						RegistrationStatus.ACTIVE, true, option.getId()) > 0) {
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

	@Transactional
	public TrainingVersionResponse duplicateVersion(UUID versionId) {
		TrainingVersion source = findVersion(versionId);
		Training training = trainingRepository.findForUpdateByIdAndOrganizationId(source.getTrainingId(), DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O treinamento informado não existe."));
		return TrainingVersionResponse.from(duplicateVersionInternal(source, training));
	}

	@Transactional(readOnly = true)
	public ContentSummaryResponse contentSummary(UUID versionId) {
		TrainingVersion version = findVersion(versionId);
		List<TrainingModule> modules = moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId);
		int activeModules = 0; int requiredVideos = 0; int activeQuestionnaires = 0; int activeQuestions = 0;
		List<String> violations = new ArrayList<>();
		for (TrainingModule module : modules) {
			if (module.getStatus() != RegistrationStatus.ACTIVE) continue;
			activeModules++;
			for (Video video : videoRepository.findAllByModuleIdOrderByDisplayOrder(module.getId())) {
				if (video.getStatus() == RegistrationStatus.ACTIVE && video.isRequired()) requiredVideos++;
				if (video.getStatus() == RegistrationStatus.ACTIVE && video.getStorageObjectKey().isBlank())
					violations.add("Vídeo ativo sem referência de arquivo.");
			}
			var questionnaire = questionnaireRepository.findByModuleId(module.getId())
					.filter(item -> item.getStatus() == RegistrationStatus.ACTIVE);
			if (questionnaire.isPresent()) {
				activeQuestionnaires++;
				List<Question> questions = questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaire.get().getId())
						.stream().filter(item -> item.getStatus() == RegistrationStatus.ACTIVE).toList();
				activeQuestions += questions.size();
				if (questions.isEmpty()) violations.add("Questionário ativo sem questões ativas.");
				for (Question question : questions) {
					List<AnswerOption> options = answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(question.getId())
							.stream().filter(item -> item.getStatus() == RegistrationStatus.ACTIVE).toList();
					if (options.size() < 2 || options.stream().filter(AnswerOption::isCorrect).count() != 1)
						violations.add("Questão ativa sem duas alternativas e uma única resposta correta.");
				}
			}
		}
		if (activeModules == 0) violations.add("Nenhum módulo ativo.");
		if (requiredVideos == 0) violations.add("Nenhum vídeo obrigatório ativo.");
		return new ContentSummaryResponse(versionId, activeModules, requiredVideos, activeQuestionnaires,
				activeQuestions, violations.isEmpty(), List.copyOf(violations));
	}

	@Transactional
	public List<ModuleResponse> reorderModules(UUID versionId, OrderRequest request) {
		draftVersion(versionId);
		List<TrainingModule> items = moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId);
		validateOrder(items.stream().map(TrainingModule::getId).toList(), request);
		Map<UUID, Integer> order = requestedOrder(request); int temporary = temporaryOrder(items.stream().map(TrainingModule::getDisplayOrder).toList());
		for (int index = 0; index < items.size(); index++) items.get(index).changeOrder(temporary + index);
		moduleRepository.flush();
		items.forEach(item -> item.changeOrder(order.get(item.getId()))); moduleRepository.flush();
		return items.stream().sorted(Comparator.comparingInt(TrainingModule::getDisplayOrder)).map(ModuleResponse::from).toList();
	}

	@Transactional
	public List<VideoResponse> reorderVideos(UUID moduleId, OrderRequest request) {
		TrainingModule module = findModule(moduleId); draftVersion(module.getTrainingVersionId());
		List<Video> items = videoRepository.findAllByModuleIdOrderByDisplayOrder(moduleId);
		validateOrder(items.stream().map(Video::getId).toList(), request);
		Map<UUID, Integer> order = requestedOrder(request); int temporary = temporaryOrder(items.stream().map(Video::getDisplayOrder).toList());
		for (int index = 0; index < items.size(); index++) items.get(index).changeOrder(temporary + index);
		videoRepository.flush(); items.forEach(item -> item.changeOrder(order.get(item.getId()))); videoRepository.flush();
		return items.stream().sorted(Comparator.comparingInt(Video::getDisplayOrder)).map(VideoResponse::from).toList();
	}

	@Transactional
	public List<QuestionResponse> reorderQuestions(UUID questionnaireId, OrderRequest request) {
		findQuestionnaireInDraft(questionnaireId);
		List<Question> items = questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaireId);
		validateOrder(items.stream().map(Question::getId).toList(), request);
		Map<UUID, Integer> order = requestedOrder(request); int temporary = temporaryOrder(items.stream().map(Question::getDisplayOrder).toList());
		for (int index = 0; index < items.size(); index++) items.get(index).changeOrder(temporary + index);
		questionRepository.flush(); items.forEach(item -> item.changeOrder(order.get(item.getId()))); questionRepository.flush();
		return items.stream().sorted(Comparator.comparingInt(Question::getDisplayOrder)).map(QuestionResponse::from).toList();
	}

	@Transactional
	public List<AnswerOptionResponse> reorderAnswerOptions(UUID questionId, OrderRequest request) {
		Question question = findQuestion(questionId); findQuestionnaireInDraft(question.getQuestionnaireId());
		List<AnswerOption> items = answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(questionId);
		validateOrder(items.stream().map(AnswerOption::getId).toList(), request);
		Map<UUID, Integer> order = requestedOrder(request); int temporary = temporaryOrder(items.stream().map(AnswerOption::getDisplayOrder).toList());
		for (int index = 0; index < items.size(); index++) items.get(index).changeOrder(temporary + index);
		answerOptionRepository.flush(); items.forEach(item -> item.changeOrder(order.get(item.getId()))); answerOptionRepository.flush();
		return items.stream().sorted(Comparator.comparingInt(AnswerOption::getDisplayOrder)).map(AnswerOptionResponse::from).toList();
	}

	@Transactional public ModuleResponse changeModuleStatus(UUID id, RegistrationStatus status) {
		TrainingModule item = findModule(id); draftVersion(item.getTrainingVersionId()); item.changeStatus(status); return ModuleResponse.from(item);
	}
	@Transactional public VideoResponse changeVideoStatus(UUID id, RegistrationStatus status) {
		Video item = findVideo(id); draftVersion(findModule(item.getModuleId()).getTrainingVersionId()); item.changeStatus(status); return VideoResponse.from(item);
	}
	@Transactional public QuestionnaireResponse changeQuestionnaireStatus(UUID id, RegistrationStatus status) {
		Questionnaire item = findQuestionnaireInDraft(id); item.changeStatus(status); return QuestionnaireResponse.from(item);
	}
	@Transactional public QuestionResponse changeQuestionStatus(UUID id, RegistrationStatus status) {
		Question item = findQuestion(id); findQuestionnaireInDraft(item.getQuestionnaireId()); item.changeStatus(status); return QuestionResponse.from(item);
	}
	@Transactional public AnswerOptionResponse changeAnswerOptionStatus(UUID id, RegistrationStatus status) {
		AnswerOption item = answerOptionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("A alternativa informada não existe."));
		findQuestionnaireInDraft(findQuestion(item.getQuestionId()).getQuestionnaireId());
		if (status == RegistrationStatus.ACTIVE && item.isCorrect()
				&& answerOptionRepository.countByQuestionIdAndStatusAndCorrectAndIdNot(item.getQuestionId(),
				RegistrationStatus.ACTIVE, true, item.getId()) > 0) throw rule("MULTIPLE_CORRECT_ANSWERS", "Cada questão deve possuir uma única alternativa correta.");
		item.changeStatus(status); return AnswerOptionResponse.from(item);
	}

	@Transactional
	public void deleteQuestionnaire(UUID moduleId) {
		TrainingModule module = findModule(moduleId); draftVersion(module.getTrainingVersionId());
		questionnaireRepository.findByModuleId(moduleId).ifPresent(questionnaire -> {
			for (Question question : questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaire.getId())) {
				answerOptionRepository.deleteAll(answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(question.getId()));
			}
			questionRepository.deleteAll(questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaire.getId()));
			questionnaireRepository.delete(questionnaire);
		});
	}
	@Transactional public void deleteQuestion(UUID id) {
		Question item = findQuestion(id); findQuestionnaireInDraft(item.getQuestionnaireId());
		answerOptionRepository.deleteAll(answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(id)); questionRepository.delete(item);
	}
	@Transactional public void deleteAnswerOption(UUID id) {
		AnswerOption item = answerOptionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("A alternativa informada não existe."));
		findQuestionnaireInDraft(findQuestion(item.getQuestionId()).getQuestionnaireId()); answerOptionRepository.delete(item);
	}

	@Override @Transactional(readOnly = true)
	public TrainingSummary requireActiveTraining(UUID trainingId) {
		Training training = findTraining(trainingId);
		if (training.getStatus() != RegistrationStatus.ACTIVE) throw rule("TRAINING_INACTIVE", "O treinamento informado está inativo.");
		return summary(training);
	}
	@Override @Transactional(readOnly = true)
	public VersionSummary requirePublishedVersion(UUID trainingId, UUID versionId) {
		requireActiveTraining(trainingId);
		TrainingVersion version = versionRepository.findByIdAndTrainingId(versionId, trainingId)
				.orElseThrow(() -> new ResourceNotFoundException("A versão informada não pertence ao treinamento."));
		if (version.getStatus() != TrainingVersionStatus.PUBLISHED)
			throw rule("VERSION_NOT_PUBLISHED", "A versão informada ainda não foi publicada.");
		return versionSummary(version);
	}
	@Override @Transactional(readOnly = true)
	public VersionSummary resolveLatestPublished(UUID trainingId) {
		requireActiveTraining(trainingId);
		TrainingVersion version = versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(trainingId,
				TrainingVersionStatus.PUBLISHED).orElseThrow(() -> rule("VERSION_NOT_PUBLISHED", "O treinamento não possui versão publicada."));
		return versionSummary(version);
	}
	@Override @Transactional(readOnly = true)
	public VersionSummary historicalVersion(UUID trainingId, UUID versionId) {
		TrainingVersion version = versionRepository.findByIdAndTrainingId(versionId, trainingId)
				.orElseThrow(() -> new ResourceNotFoundException("A versão informada não pertence ao treinamento."));
		if (version.getStatus() == TrainingVersionStatus.DRAFT)
			throw rule("VERSION_NOT_PUBLISHED", "A versão informada ainda não foi publicada.");
		return versionSummary(version);
	}
	@Override @Transactional(readOnly = true)
	public VersionSummary latestPublishedForCompliance(UUID trainingId) {
		findTraining(trainingId);
		TrainingVersion version = versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(trainingId,
				TrainingVersionStatus.PUBLISHED).orElseThrow(() -> rule("VERSION_NOT_PUBLISHED", "O treinamento não possui versão publicada."));
		return versionSummary(version);
	}
	@Override @Transactional(readOnly = true)
	public TrainingSummary summary(UUID trainingId) { return summary(findTraining(trainingId)); }
	@Override @Transactional(readOnly = true)
	public Map<UUID, TrainingSummary> summaries(java.util.Collection<UUID> trainingIds) {
		return trainingRepository.findAllById(trainingIds).stream()
				.filter(item -> item.getOrganizationId().equals(DEFAULT_ORGANIZATION_ID))
				.collect(java.util.stream.Collectors.toUnmodifiableMap(Training::getId, this::summary));
	}
	@Override @Transactional(readOnly = true)
	public CompletionRules completionRules(UUID trainingId, UUID versionId) {
		TrainingVersion version = versionRepository.findByIdAndTrainingId(versionId, trainingId)
				.orElseThrow(() -> new ResourceNotFoundException("A versão informada não pertence ao treinamento."));
		if (version.getStatus() == TrainingVersionStatus.DRAFT)
			throw rule("VERSION_NOT_PUBLISHED", "A versão informada ainda não foi publicada.");
		return new CompletionRules(trainingId, versionId, version.getVersionNumber(), version.getStatus(),
				version.getValidityType(), version.getValidityValue(), version.getPassingScore(),
				version.getMaxAttempts(), version.getRetryIntervalMinutes());
	}

	@Override @Transactional(readOnly = true)
	public ExecutionContent content(UUID trainingVersionId) {
		TrainingVersion version = findVersion(trainingVersionId);
		if (version.getStatus() == TrainingVersionStatus.DRAFT)
			throw rule("VERSION_NOT_PUBLISHED", "A versão atribuída ainda não foi publicada.");
		List<ExecutionModule> modules = moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(trainingVersionId).stream()
				.filter(module -> module.getStatus() == RegistrationStatus.ACTIVE)
				.map(module -> new ExecutionModule(module.getId(), module.getTitle(), module.getDescription(), module.getDisplayOrder(),
						videoRepository.findAllByModuleIdOrderByDisplayOrder(module.getId()).stream()
								.filter(video -> video.getStatus() == RegistrationStatus.ACTIVE)
								.map(video -> executionVideo(video, trainingVersionId)).toList(),
						questionnaireRepository.findByModuleId(module.getId())
								.filter(questionnaire -> questionnaire.getStatus() == RegistrationStatus.ACTIVE)
								.map(questionnaire -> new ExecutionQuestionnaire(questionnaire.getId(), questionnaire.getTitle(),
										module.getDisplayOrder())).orElse(null)))
				.toList();
		return new ExecutionContent(trainingVersionId, modules);
	}

	@Override @Transactional(readOnly = true)
	public ExecutionVideo requireVideo(UUID trainingVersionId, UUID videoId) {
		Video video = findVideo(videoId);
		TrainingModule module = findModule(video.getModuleId());
		if (!module.getTrainingVersionId().equals(trainingVersionId) || module.getStatus() != RegistrationStatus.ACTIVE
				|| video.getStatus() != RegistrationStatus.ACTIVE)
			throw new ResourceNotFoundException("O vídeo não pertence à versão atribuída.");
		TrainingVersion version = findVersion(trainingVersionId);
		if (version.getStatus() == TrainingVersionStatus.DRAFT)
			throw new ResourceNotFoundException("O vídeo não pertence a uma versão executável.");
		return executionVideo(video, trainingVersionId);
	}

	@Override @Transactional(readOnly = true)
	public ExecutionVideo requireVideo(UUID videoId) {
		Video video = findVideo(videoId);
		TrainingModule module = findModule(video.getModuleId());
		if (module.getStatus() != RegistrationStatus.ACTIVE || video.getStatus() != RegistrationStatus.ACTIVE)
			throw new ResourceNotFoundException("O vídeo não está ativo.");
		TrainingVersion version = findVersion(module.getTrainingVersionId());
		if (version.getStatus() == TrainingVersionStatus.DRAFT)
			throw new ResourceNotFoundException("O vídeo não pertence a uma versão executável.");
		return executionVideo(video, module.getTrainingVersionId());
	}

	@Override @Transactional(readOnly = true)
	public ExecutionQuestionnaireDetail requireQuestionnaire(UUID trainingVersionId, UUID questionnaireId) {
		Questionnaire questionnaire = findQuestionnaire(questionnaireId);
		TrainingModule module = findModule(questionnaire.getModuleId());
		TrainingVersion version = findVersion(trainingVersionId);
		if (!module.getTrainingVersionId().equals(trainingVersionId)
				|| version.getStatus() == TrainingVersionStatus.DRAFT
				|| module.getStatus() != RegistrationStatus.ACTIVE
				|| questionnaire.getStatus() != RegistrationStatus.ACTIVE)
			throw new ResourceNotFoundException("O questionário não pertence à versão atribuída.");
		List<ExecutionQuestion> questions = questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaireId).stream()
				.filter(question -> question.getStatus() == RegistrationStatus.ACTIVE)
				.map(question -> new ExecutionQuestion(question.getId(), question.getStatement(), question.getDisplayOrder(),
						answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(question.getId()).stream()
								.filter(option -> option.getStatus() == RegistrationStatus.ACTIVE)
								.map(option -> new ExecutionOption(option.getId(), option.getText(), option.getDisplayOrder(), option.isCorrect()))
								.toList()))
				.toList();
		if (questions.isEmpty()) throw rule("QUESTIONNAIRE_EMPTY", "O questionário não possui questões ativas.");
		return new ExecutionQuestionnaireDetail(questionnaire.getId(), trainingVersionId, questionnaire.getTitle(),
				questionnaire.getPassingScore().max(version.getPassingScore()).max(BigDecimal.valueOf(70)),
				questionnaire.getMaxAttempts() == null ? version.getMaxAttempts() : questionnaire.getMaxAttempts(),
				questionnaire.getRetryIntervalMinutes(), questionnaire.isShuffleQuestions(), questions);
	}

	private ExecutionVideo executionVideo(Video video, UUID trainingVersionId) {
		return new ExecutionVideo(video.getId(), video.getModuleId(), trainingVersionId, video.getTitle(),
				video.getDescription(), video.getDisplayOrder(), video.getDurationSeconds(), video.isRequired(),
				video.getFileId(), video.getStorageObjectKey());
	}

	private UploadedFileCatalog.FileReference requireVideoFile(UUID fileId, String legacyObjectKey) {
		if (fileId != null) return uploadedFiles.requireTrainingVideo(fileId);
		if (legacyObjectKey != null && !legacyObjectKey.isBlank()) {
			try {
				return uploadedFiles.requireTrainingVideo(legacyObjectKey.trim());
			} catch (ResourceNotFoundException exception) {
				if (uploadProperties == null || !uploadProperties.allowLegacyObjectKeys()) throw exception;
			}
		}
		if (uploadProperties != null && uploadProperties.allowLegacyObjectKeys()
				&& legacyObjectKey != null && !legacyObjectKey.isBlank())
			return new UploadedFileCatalog.FileReference(null, legacyObjectKey.trim(), null, 0);
		throw rule("TRAINING_VIDEO_FILE_REQUIRED", "Informe um upload de vídeo concluído.");
	}

	private TrainingVersion duplicateVersionInternal(TrainingVersion source, Training training) {
		int number = versionRepository.findMaximumVersionNumber(training.getId()) + 1;
		TrainingVersion target = versionRepository.saveAndFlush(new TrainingVersion(training, number,
				source.getWorkloadMinutes(), source.getValidityType(), source.getValidityValue(), source.getPassingScore(),
				source.getMaxAttempts(), source.getRetryIntervalMinutes()));
		for (TrainingModule sourceModule : moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(source.getId())) {
			TrainingModule targetModule = moduleRepository.saveAndFlush(new TrainingModule(target.getId(), sourceModule.getTitle(),
					sourceModule.getDescription(), sourceModule.getDisplayOrder(), sourceModule.getStatus()));
			for (Video sourceVideo : videoRepository.findAllByModuleIdOrderByDisplayOrder(sourceModule.getId())) {
				videoRepository.save(new Video(targetModule.getId(), sourceVideo.getTitle(), sourceVideo.getDescription(),
						sourceVideo.getDisplayOrder(), sourceVideo.getDurationSeconds(), sourceVideo.getFileId(), sourceVideo.getStorageObjectKey(),
						sourceVideo.isRequired(), sourceVideo.getStatus()));
			}
			questionnaireRepository.findByModuleId(sourceModule.getId()).ifPresent(sourceQuestionnaire -> {
				Questionnaire targetQuestionnaire = questionnaireRepository.saveAndFlush(new Questionnaire(targetModule.getId(),
						sourceQuestionnaire.getTitle(), sourceQuestionnaire.getPassingScore(), sourceQuestionnaire.getMaxAttempts(),
						sourceQuestionnaire.getRetryIntervalMinutes(), sourceQuestionnaire.isShuffleQuestions(), sourceQuestionnaire.getStatus()));
				for (Question sourceQuestion : questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(sourceQuestionnaire.getId())) {
					Question targetQuestion = questionRepository.saveAndFlush(new Question(targetQuestionnaire.getId(),
							sourceQuestion.getStatement(), sourceQuestion.getDisplayOrder(), sourceQuestion.getStatus()));
					for (AnswerOption sourceOption : answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(sourceQuestion.getId())) {
						answerOptionRepository.save(new AnswerOption(targetQuestion.getId(), sourceOption.getText(),
								sourceOption.isCorrect(), sourceOption.getDisplayOrder(), sourceOption.getStatus()));
					}
				}
			});
		}
		return target;
	}

	private String publicationSnapshot(TrainingVersion version) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("trainingName", version.getTrainingNameSnapshot());
		snapshot.put("trainingCode", version.getTrainingCodeSnapshot());
		snapshot.put("trainingDescription", version.getTrainingDescriptionSnapshot());
		snapshot.put("trainingCategory", version.getTrainingCategorySnapshot());
		snapshot.put("regulatoryStandard", version.isRegulatoryStandardSnapshot());
		snapshot.put("versionNumber", version.getVersionNumber());
		snapshot.put("workloadMinutes", version.getWorkloadMinutes());
		snapshot.put("validityType", version.getValidityType().name());
		snapshot.put("validityValue", version.getValidityValue());
		snapshot.put("passingScore", version.getPassingScore());
		snapshot.put("maxAttempts", version.getMaxAttempts());
		snapshot.put("retryIntervalMinutes", version.getRetryIntervalMinutes());
		List<Map<String, Object>> modules = new ArrayList<>();
		for (TrainingModule module : moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(version.getId())) {
			Map<String, Object> moduleData = new LinkedHashMap<>();
			moduleData.put("id", module.getId()); moduleData.put("title", module.getTitle());
			moduleData.put("description", module.getDescription()); moduleData.put("order", module.getDisplayOrder());
			moduleData.put("status", module.getStatus().name());
			moduleData.put("videos", videoRepository.findAllByModuleIdOrderByDisplayOrder(module.getId()).stream()
					.map(video -> Map.of("id", video.getId(), "title", video.getTitle(), "order", video.getDisplayOrder(),
							"durationSeconds", video.getDurationSeconds(), "storageObjectKey", video.getStorageObjectKey(),
							"required", video.isRequired(), "status", video.getStatus().name())).toList());
			questionnaireRepository.findByModuleId(module.getId()).ifPresent(questionnaire -> {
				Map<String, Object> questionnaireData = new LinkedHashMap<>();
				questionnaireData.put("id", questionnaire.getId()); questionnaireData.put("title", questionnaire.getTitle());
				questionnaireData.put("passingScore", questionnaire.getPassingScore());
				questionnaireData.put("maxAttempts", questionnaire.getMaxAttempts());
				questionnaireData.put("retryIntervalMinutes", questionnaire.getRetryIntervalMinutes());
				questionnaireData.put("shuffleQuestions", questionnaire.isShuffleQuestions());
				questionnaireData.put("status", questionnaire.getStatus().name());
				List<Map<String, Object>> questions = new ArrayList<>();
				for (Question question : questionRepository.findAllByQuestionnaireIdOrderByDisplayOrder(questionnaire.getId())) {
					Map<String, Object> questionData = new LinkedHashMap<>();
					questionData.put("id", question.getId()); questionData.put("statement", question.getStatement());
					questionData.put("order", question.getDisplayOrder()); questionData.put("status", question.getStatus().name());
					questionData.put("options", answerOptionRepository.findAllByQuestionIdOrderByDisplayOrder(question.getId()).stream()
							.map(option -> Map.of("id", option.getId(), "text", option.getText(), "correct", option.isCorrect(),
									"order", option.getDisplayOrder(), "status", option.getStatus().name())).toList());
					questions.add(questionData);
				}
				questionnaireData.put("questions", questions); moduleData.put("questionnaire", questionnaireData);
			});
			modules.add(moduleData);
		}
		snapshot.put("modules", modules);
		try { return JSON.writeValueAsString(snapshot); }
		catch (JsonProcessingException exception) { throw new IllegalStateException("Could not snapshot training content", exception); }
	}

	private void validateOrder(List<UUID> existingIds, OrderRequest request) {
		Set<UUID> ids = request.items().stream().map(OrderRequest.Item::id).collect(java.util.stream.Collectors.toSet());
		Set<Integer> orders = request.items().stream().map(OrderRequest.Item::order).collect(java.util.stream.Collectors.toSet());
		if (ids.size() != request.items().size() || orders.size() != request.items().size()
				|| !ids.equals(Set.copyOf(existingIds))) {
			throw rule("INVALID_CONTENT_ORDER", "A reordenação deve informar todos os itens uma única vez, com ordens únicas.");
		}
	}
	private Map<UUID, Integer> requestedOrder(OrderRequest request) {
		return request.items().stream().collect(java.util.stream.Collectors.toMap(OrderRequest.Item::id, OrderRequest.Item::order));
	}
	private int temporaryOrder(List<Integer> orders) { return orders.stream().max(Integer::compareTo).orElse(0) + orders.size() + 1000; }
	private TrainingSummary summary(Training training) { return new TrainingSummary(training.getId(), training.getName(), training.getCode(),
			training.getDescription(), training.getCategory(), training.isRegulatoryStandard(), training.getStatus()); }
	private VersionSummary versionSummary(TrainingVersion version) {
		return new VersionSummary(version.getId(), version.getTrainingId(), version.getVersionNumber(), version.getStatus());
	}
	private void validatePassingScore(BigDecimal passingScore) {
		if (passingScore == null || passingScore.compareTo(BigDecimal.valueOf(70)) < 0
				|| passingScore.compareTo(BigDecimal.valueOf(100)) > 0)
			throw rule("INVALID_PASSING_SCORE", "A nota mínima deve estar entre 70 e 100.");
	}

	private void validatePublicationContent(TrainingVersion version) {
		List<TrainingModule> modules = moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(version.getId()).stream()
				.filter(module -> module.getStatus() == RegistrationStatus.ACTIVE).toList();
		if (modules.isEmpty()) {
			throw rule("VERSION_CONTENT_INVALID", "A versão deve possuir ao menos um módulo ativo.");
		}
		long requiredVideos = 0;
		for (TrainingModule module : modules) {
			for (Video video : videoRepository.findAllByModuleIdOrderByDisplayOrder(module.getId())) {
				if (video.getStatus() == RegistrationStatus.ACTIVE
						&& (video.getStorageObjectKey().isBlank() || video.getDurationSeconds() <= 0)) {
					throw rule("VERSION_CONTENT_INVALID", "Todos os vídeos ativos devem possuir duração e arquivo válidos.");
				}
				if (video.getStatus() == RegistrationStatus.ACTIVE && video.isRequired()) requiredVideos++;
			}
			questionnaireRepository.findByModuleId(module.getId())
					.filter(questionnaire -> questionnaire.getStatus() == RegistrationStatus.ACTIVE)
					.ifPresent(questionnaire -> validateQuestionnaire(questionnaire));
		}
		if (requiredVideos == 0) {
			throw rule("VERSION_CONTENT_INVALID", "A versão online deve possuir ao menos um vídeo obrigatório ativo.");
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
		if (passingScore == null || passingScore.compareTo(BigDecimal.valueOf(70)) < 0
				|| passingScore.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw rule("INVALID_PASSING_SCORE", "A nota mínima deve estar entre 70 e 100.");
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
