package dev.igorbarbosa.worktrainingsystem.trainings.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.trainings.api.CreateTrainingRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.api.TrainingVersionRequest;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.Training;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingModule;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersion;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.TrainingVersionStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.AnswerOptionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.QuestionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.QuestionnaireRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingModuleRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingVersionRepository;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.VideoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
		when(videoRepository.findAllByModuleIdOrderByDisplayOrder(any(UUID.class))).thenReturn(List.of());
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
}
