package dev.igorbarbosa.worktrainingsystem.trainings.persistence;

import dev.igorbarbosa.worktrainingsystem.trainings.domain.Training;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface TrainingRepository extends JpaRepository<Training, UUID>, JpaSpecificationExecutor<Training> {

	boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

	Optional<Training> findByIdAndOrganizationId(UUID id, UUID organizationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Training> findForUpdateByIdAndOrganizationId(UUID id, UUID organizationId);
}
