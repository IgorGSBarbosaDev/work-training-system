package dev.igorbarbosa.worktrainingsystem.assignments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentReasonRequest(@NotBlank @Size(max = 1000) String reason) {
}
