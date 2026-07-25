package dev.igorbarbosa.worktrainingsystem.files.persistence;

import dev.igorbarbosa.worktrainingsystem.files.domain.FileState;
import dev.igorbarbosa.worktrainingsystem.files.domain.UploadedFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
	Optional<UploadedFile> findByIdAndOrganizationId(UUID id, UUID organizationId);
	Optional<UploadedFile> findByOrganizationIdAndObjectKey(UUID organizationId, String objectKey);
	List<UploadedFile> findTop100ByStateAndExpiresAtBeforeOrderByExpiresAt(FileState state, Instant expiresAt);
}
