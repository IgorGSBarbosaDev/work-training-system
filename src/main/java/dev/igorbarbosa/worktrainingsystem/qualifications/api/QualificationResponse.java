package dev.igorbarbosa.worktrainingsystem.qualifications.api;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationBlockingType;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QualificationResponse(UUID id, Employee employee, Activity activity, QualificationStatus status,
		Instant calculatedAt, LocalDate nextExpirationDate, List<BlockingReason> blockingReasons, String disclaimer) {
	public static final String DISCLAIMER = "A qualificação é baseada nos treinamentos registrados e não substitui liberações médicas, operacionais ou legais externas.";
	public record Employee(UUID id, String name, String registration) {}
	public record Activity(UUID id, String name) {}
	public record BlockingReason(QualificationBlockingType type, UUID requirementId, UUID trainingId,
			String trainingName, UUID requiredVersionId, UUID completionVersionId,
			LocalDate expirationDate, AssignmentStatus assignmentStatus) {}
}
