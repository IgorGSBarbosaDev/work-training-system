package dev.igorbarbosa.worktrainingsystem.trainings.api;

import java.util.List;
import java.util.UUID;

public record ContentSummaryResponse(UUID versionId, int activeModules, int activeRequiredVideos,
		int activeQuestionnaires, int activeQuestions, boolean publishable, List<String> violations) {}
