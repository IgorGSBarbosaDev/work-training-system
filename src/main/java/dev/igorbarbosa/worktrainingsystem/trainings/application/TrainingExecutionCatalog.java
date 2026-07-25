package dev.igorbarbosa.worktrainingsystem.trainings.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public, read-only view of content pinned to an assigned training version. */
public interface TrainingExecutionCatalog {
	ExecutionContent content(UUID trainingVersionId);
	ExecutionVideo requireVideo(UUID trainingVersionId, UUID videoId);
	ExecutionVideo requireVideo(UUID videoId);
	ExecutionQuestionnaireDetail requireQuestionnaire(UUID trainingVersionId, UUID questionnaireId);

	record ExecutionContent(UUID trainingVersionId, List<ExecutionModule> modules) {
		public List<ExecutionVideo> activeVideos() {
			return modules.stream().flatMap(module -> module.videos().stream()).toList();
		}
		public boolean hasActiveQuestionnaires() {
			return modules.stream().anyMatch(module -> module.questionnaire() != null);
		}
	}
	record ExecutionModule(UUID id, String title, String description, int order,
			List<ExecutionVideo> videos, ExecutionQuestionnaire questionnaire) {}
	record ExecutionVideo(UUID id, UUID moduleId, UUID trainingVersionId, String title, String description,
			int order, int durationSeconds, boolean required, UUID fileId, String objectKey) {}
	record ExecutionQuestionnaire(UUID id, String title, int order) {}
	record ExecutionQuestionnaireDetail(UUID id, UUID trainingVersionId, String title, BigDecimal passingScore,
			Integer maxAttempts, int retryIntervalMinutes, boolean shuffleQuestions,
			List<ExecutionQuestion> questions) {}
	record ExecutionQuestion(UUID id, String statement, int order, List<ExecutionOption> options) {}
	record ExecutionOption(UUID id, String text, int order, boolean correct) {}
}
