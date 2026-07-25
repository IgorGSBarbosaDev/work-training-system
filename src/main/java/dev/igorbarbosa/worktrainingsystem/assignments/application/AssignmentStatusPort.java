package dev.igorbarbosa.worktrainingsystem.assignments.application;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentStatusPort {
	Optional<AssignmentStatus> effectiveStatus(UUID employeeId, UUID trainingId, UUID trainingVersionId);
}
