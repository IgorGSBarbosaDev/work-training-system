package dev.igorbarbosa.worktrainingsystem.assessments.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AssessmentAttemptRequest(@NotEmpty List<@Valid Answer> answers) {
	public record Answer(@NotNull UUID questionId, @NotNull UUID answerOptionId) {}
}
