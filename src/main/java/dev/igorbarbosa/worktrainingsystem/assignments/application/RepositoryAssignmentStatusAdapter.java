package dev.igorbarbosa.worktrainingsystem.assignments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RepositoryAssignmentStatusAdapter implements AssignmentStatusPort {
	private static final EnumSet<AssignmentStatus> EFFECTIVE = EnumSet.of(AssignmentStatus.NOT_STARTED,
			AssignmentStatus.IN_PROGRESS, AssignmentStatus.AWAITING_ASSESSMENT,
			AssignmentStatus.APPROVED, AssignmentStatus.FAILED);
	private final TrainingAssignmentRepository assignments;
	RepositoryAssignmentStatusAdapter(TrainingAssignmentRepository assignments) { this.assignments = assignments; }
	@Override @Transactional(readOnly = true)
	public Optional<AssignmentStatus> effectiveStatus(UUID employeeId, UUID trainingId, UUID versionId) {
		return assignments.findFirstByOrganizationIdAndEmployeeIdAndTrainingIdAndTrainingVersionIdAndStatusIn(
				DEFAULT_ORGANIZATION_ID, employeeId, trainingId, versionId, EFFECTIVE)
				.map(item -> item.getStatus());
	}
}
