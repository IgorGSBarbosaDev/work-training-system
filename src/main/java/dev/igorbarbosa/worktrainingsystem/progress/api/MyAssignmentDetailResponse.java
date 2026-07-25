package dev.igorbarbosa.worktrainingsystem.progress.api;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentResponse;

public record MyAssignmentDetailResponse(@JsonUnwrapped AssignmentResponse assignment,
		LearningPathResponse learningPath, ResumePointResponse resumePoint) {
}
