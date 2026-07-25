package dev.igorbarbosa.worktrainingsystem.progress.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LearningPathResponse(UUID assignmentId, UUID trainingVersionId, AssignmentStatus assignmentStatus,
		List<Module> modules, Assessment assessment) {
	public record Module(UUID id, String title, String description, int order, List<Video> videos, Questionnaire questionnaire) {}
	public record Video(UUID id, String title, String description, int order, int durationSeconds, boolean required,
			long positionSeconds, BigDecimal percentageWatched, boolean completed) {}
	public record Questionnaire(UUID id, String title, boolean available) {}
	public record Assessment(boolean required, boolean available, String summary) {}
}
