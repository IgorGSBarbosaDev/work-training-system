package dev.igorbarbosa.worktrainingsystem.trainings.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.trainings.api.CreateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.ModuleRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.AnswerOptionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.OrderRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.AnswerOption;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TrainingCatalogServiceTest {

	@Mock
	private TrainingRepository trainingRepository;
	@Mock
	private TrainingVersionRepository versionRepository;
	@Mock
	private TrainingModuleRepository moduleRepository;
	@Mock
	private VideoRepository videoRepository;
	@Mock
	private QuestionnaireRepository questionnaireRepository;
	@Mock
	private QuestionRepository questionRepository;
	@Mock
	private AnswerOptionRepository answerOptionRepository;

	@InjectMocks
	private TrainingCatalogService service;

	@Test
	void createsTrainingWithInitialDraftVersion() {
		when(trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, "NR11"))
				.thenReturn(false);
		when(trainingRepository.saveAndFlush(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionRepository.saveAndFlush(any(TrainingVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(new CreateTrainingRequest(" NR-11 ", "nr11", "Descrição", "NR", true,
				RegistrationStatus.ACTIVE, new CreateTrainingRequest.InitialVersion(480, ValidityType.MONTHS, 24,
						BigDecimal.valueOf(70), 3, 0)));

		ArgumentCaptor<TrainingVersion> captor = ArgumentCaptor.forClass(TrainingVersion.class);
		verify(versionRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getVersionNumber()).isEqualTo(1);
		assertThat(captor.getValue().getStatus()).isEqualTo(TrainingVersionStatus.DRAFT);
		assertThat(captor.getValue().getValidityValue()).isEqualTo(24);
	}

	@Test
	void rejectsValidityValueForIndefiniteVersion() {
		when(trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, "SEG01"))
				.thenReturn(false);

		assertThatThrownBy(() -> service.create(new CreateTrainingRequest("Segurança", "SEG01", null, null, false,
				RegistrationStatus.ACTIVE, new CreateTrainingRequest.InitialVersion(60, ValidityType.INDEFINITE, 1,
						BigDecimal.valueOf(70), null, 0))))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("INVALID_VALIDITY");
	}

	@Test
	void rejectsPassingScoreBelowSeventy() {
		when(trainingRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, "SEG01"))
				.thenReturn(false);
		assertThatThrownBy(() -> service.create(new CreateTrainingRequest("Segurança", "SEG01", null, null, false,
				RegistrationStatus.ACTIVE, new CreateTrainingRequest.InitialVersion(60, ValidityType.INDEFINITE, null,
						BigDecimal.valueOf(69.99), null, 0))))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("INVALID_PASSING_SCORE");
	}

	@Test
	void publishesVersionWithValidContent() {
		UUID trainingId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID moduleId = UUID.randomUUID();
		Training training = new Training(DEFAULT_ORGANIZATION_ID, "Segurança", "SEG01", null, null, false,
				RegistrationStatus.ACTIVE);
		TrainingVersion version = new TrainingVersion(trainingId, 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), null, 0);
		ReflectionTestUtils.setField(version, "id", versionId);
		TrainingModule module = new TrainingModule(versionId, "Fundamentos", null, 1, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(module, "id", moduleId);
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
		when(trainingRepository.findByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(training));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(any(UUID.class))).thenReturn(List.of(module));
		Video video = new Video(moduleId, "Obrigatório", null, 1, 60, "videos/required.mp4", true,
				RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
		when(videoRepository.findAllByModuleIdOrderByDisplayOrder(any(UUID.class))).thenReturn(List.of(video));
		when(questionnaireRepository.findByModuleId(any(UUID.class))).thenReturn(Optional.empty());
		when(versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(trainingId,
				TrainingVersionStatus.PUBLISHED)).thenReturn(Optional.empty());
		when(versionRepository.saveAndFlush(any(TrainingVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.publishVersion(versionId).status()).isEqualTo(TrainingVersionStatus.PUBLISHED);
	}

	@Test
	void rejectsVersionWithoutActiveModule() {
		UUID trainingId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		Training training = new Training(DEFAULT_ORGANIZATION_ID, "Segurança", "SEG01", null, null, false,
				RegistrationStatus.ACTIVE);
		TrainingVersion version = new TrainingVersion(trainingId, 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), null, 0);
		ReflectionTestUtils.setField(version, "id", versionId);
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
		when(trainingRepository.findByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(training));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(any(UUID.class))).thenReturn(List.of());

		assertThatThrownBy(() -> service.publishVersion(versionId))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("VERSION_CONTENT_INVALID");
	}

	@Test
	void preventsCorrectAnswerActivationLoophole() {
		UUID questionId = UUID.randomUUID(); UUID questionnaireId = UUID.randomUUID(); UUID optionId = UUID.randomUUID();
		dev.igorbarbosa.worktrainingsystem.trainings.domain.Question question =
				new dev.igorbarbosa.worktrainingsystem.trainings.domain.Question(questionnaireId, "Questão", 1, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(question, "id", questionId);
		AnswerOption option = new AnswerOption(questionId, "Correta B", true, 2, RegistrationStatus.INACTIVE);
		ReflectionTestUtils.setField(option, "id", optionId);
		UUID moduleId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
		dev.igorbarbosa.worktrainingsystem.trainings.domain.Questionnaire questionnaire =
				new dev.igorbarbosa.worktrainingsystem.trainings.domain.Questionnaire(moduleId, "Avaliação", BigDecimal.valueOf(70), 3, 0, false, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(questionnaire, "id", questionnaireId);
		TrainingModule module = new TrainingModule(versionId, "Módulo", null, 1, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(module, "id", moduleId);
		TrainingVersion version = new TrainingVersion(UUID.randomUUID(), 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), 3, 0);
		ReflectionTestUtils.setField(version, "id", versionId);
		when(answerOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
		when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
		when(questionnaireRepository.findById(questionnaireId)).thenReturn(Optional.of(questionnaire));
		when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
		when(answerOptionRepository.countByQuestionIdAndStatusAndCorrectAndIdNot(questionId,
				RegistrationStatus.ACTIVE, true, optionId)).thenReturn(1L);

		assertThatThrownBy(() -> service.updateAnswerOption(optionId,
				new AnswerOptionRequest("Correta B", true, 2, RegistrationStatus.ACTIVE)))
				.isInstanceOf(BusinessRuleViolationException.class).extracting("code").isEqualTo("MULTIPLE_CORRECT_ANSWERS");
	}

	@Test
	void archivesAndFlushesCurrentPublicationBeforePublishingReplacement() {
		UUID trainingId = UUID.randomUUID(); UUID versionId = UUID.randomUUID(); UUID moduleId = UUID.randomUUID();
		Training training = new Training(DEFAULT_ORGANIZATION_ID, "Segurança", "SEG01", null, null, false, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(training, "id", trainingId);
		TrainingVersion replacement = new TrainingVersion(training, 2, 60, ValidityType.INDEFINITE, null, BigDecimal.valueOf(70), null, 0);
		ReflectionTestUtils.setField(replacement, "id", versionId);
		TrainingVersion current = new TrainingVersion(training, 1, 60, ValidityType.INDEFINITE, null, BigDecimal.valueOf(70), null, 0);
		current.capturePublicationSnapshot(training, "{}"); current.publish(Instant.parse("2026-07-23T12:00:00Z"));
		TrainingModule module = new TrainingModule(versionId, "Fundamentos", null, 1, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(module, "id", moduleId);
		Video video = new Video(moduleId, "Obrigatório", null, 1, 60, "video.mp4", true, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(replacement));
		when(trainingRepository.findByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(training));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId)).thenReturn(List.of(module));
		when(videoRepository.findAllByModuleIdOrderByDisplayOrder(moduleId)).thenReturn(List.of(video));
		when(questionnaireRepository.findByModuleId(moduleId)).thenReturn(Optional.empty());
		when(versionRepository.findFirstByTrainingIdAndStatusOrderByVersionNumberDesc(trainingId,
				TrainingVersionStatus.PUBLISHED)).thenReturn(Optional.of(current));
		when(versionRepository.saveAndFlush(replacement)).thenReturn(replacement);

		service.publishVersion(versionId);

		InOrder order = inOrder(versionRepository);
		order.verify(versionRepository).flush();
		order.verify(versionRepository).saveAndFlush(replacement);
		assertThat(current.getStatus()).isEqualTo(TrainingVersionStatus.ARCHIVED);
	}

	@Test
	void publishedSnapshotCannotBeRefreshed() {
		Training training = new Training(DEFAULT_ORGANIZATION_ID, "Original", "ORI", "Descrição", "Categoria", true,
				RegistrationStatus.ACTIVE);
		TrainingVersion version = new TrainingVersion(training, 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), null, 0);
		version.capturePublicationSnapshot(training, "{\"modules\":[]}"); version.publish(Instant.now());
		training.update("Alterado", "ALT", null, null, false);

		assertThatThrownBy(() -> version.refreshDraftSnapshot(training)).isInstanceOf(IllegalStateException.class);
		assertThat(version.getTrainingNameSnapshot()).isEqualTo("Original");
	}

	@Test
	void reordersAllModulesWithoutUniqueOrderCollision() {
		UUID versionId = UUID.randomUUID();
		TrainingVersion version = new TrainingVersion(UUID.randomUUID(), 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), null, 0); ReflectionTestUtils.setField(version, "id", versionId);
		TrainingModule first = new TrainingModule(versionId, "Primeiro", null, 1, RegistrationStatus.ACTIVE);
		TrainingModule second = new TrainingModule(versionId, "Segundo", null, 2, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(first, "id", UUID.randomUUID()); ReflectionTestUtils.setField(second, "id", UUID.randomUUID());
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId)).thenReturn(List.of(first, second));

		var result = service.reorderModules(versionId, new OrderRequest(List.of(
				new OrderRequest.Item(first.getId(), 2), new OrderRequest.Item(second.getId(), 1))));

		assertThat(result).extracting(item -> item.id()).containsExactly(second.getId(), first.getId());
		verify(moduleRepository, org.mockito.Mockito.times(2)).flush();
	}

	@Test
	void duplicatesVersionAsNextDraft() {
		UUID trainingId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID();
		Training training = new Training(DEFAULT_ORGANIZATION_ID, "Segurança", "SEG", null, null, false,
				RegistrationStatus.ACTIVE); ReflectionTestUtils.setField(training, "id", trainingId);
		TrainingVersion source = new TrainingVersion(training, 1, 90, ValidityType.MONTHS, 12,
				BigDecimal.valueOf(75), 3, 10); ReflectionTestUtils.setField(source, "id", sourceId);
		when(versionRepository.findById(sourceId)).thenReturn(Optional.of(source));
		when(trainingRepository.findForUpdateByIdAndOrganizationId(trainingId, DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(training));
		when(versionRepository.findMaximumVersionNumber(trainingId)).thenReturn(1);
		when(versionRepository.saveAndFlush(any(TrainingVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(sourceId)).thenReturn(List.of());

		var duplicate = service.duplicateVersion(sourceId);

		assertThat(duplicate.versionNumber()).isEqualTo(2);
		assertThat(duplicate.status()).isEqualTo(TrainingVersionStatus.DRAFT);
		assertThat(duplicate.passingScore()).isEqualByComparingTo("75");
	}

	@Test
	void rejectsDuplicateModuleOrderBeforeDatabaseConstraint() {
		UUID versionId = UUID.randomUUID();
		TrainingVersion version = new TrainingVersion(UUID.randomUUID(), 1, 60, ValidityType.INDEFINITE, null,
				BigDecimal.valueOf(70), null, 0);
		ReflectionTestUtils.setField(version, "id", versionId);
		TrainingModule existing = new TrainingModule(versionId, "Existente", null, 1, RegistrationStatus.ACTIVE);
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
		when(moduleRepository.findAllByTrainingVersionIdOrderByDisplayOrder(versionId)).thenReturn(List.of(existing));

		assertThatThrownBy(() -> service.createModule(versionId,
				new ModuleRequest("Novo", null, 1, RegistrationStatus.ACTIVE)))
				.isInstanceOf(BusinessRuleViolationException.class)
				.extracting("code").isEqualTo("INVALID_CONTENT_ORDER");
	}
}
