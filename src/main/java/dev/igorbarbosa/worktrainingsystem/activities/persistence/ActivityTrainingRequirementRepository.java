package dev.igorbarbosa.worktrainingsystem.activities.persistence;

import dev.igorbarbosa.worktrainingsystem.activities.domain.ActivityTrainingRequirement;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTrainingRequirementRepository extends JpaRepository<ActivityTrainingRequirement, UUID> {
	boolean existsByOrganizationIdAndActivityIdAndTrainingIdAndStatus(
			UUID organizationId, UUID activityId, UUID trainingId, RegistrationStatus status);
	Optional<ActivityTrainingRequirement> findByIdAndActivityIdAndOrganizationId(
			UUID id, UUID activityId, UUID organizationId);
	List<ActivityTrainingRequirement> findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
			UUID organizationId, UUID activityId, RegistrationStatus status);
}
