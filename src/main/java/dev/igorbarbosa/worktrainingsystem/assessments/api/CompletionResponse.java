package dev.igorbarbosa.worktrainingsystem.assessments.api;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.CompletionForm;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CompletionResponse(UUID id, UUID employeeId, UUID trainingId, UUID trainingVersionId,
		UUID sourceAssignmentId, LocalDate completionDate, Instant completedAt, CompletionForm form,
		BigDecimal finalScore, ValidityType appliedValidityType, Integer appliedValidityValue,
		LocalDate expirationDate, UUID responsibleUserId, String notes, UUID externalEvidenceFileId) {}
