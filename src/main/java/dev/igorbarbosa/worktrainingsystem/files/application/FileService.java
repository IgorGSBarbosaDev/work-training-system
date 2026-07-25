package dev.igorbarbosa.worktrainingsystem.files.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.files.api.FileUrlResponse;
import dev.igorbarbosa.worktrainingsystem.files.api.UploadRequest;
import dev.igorbarbosa.worktrainingsystem.files.api.UploadResponse;
import dev.igorbarbosa.worktrainingsystem.files.domain.FilePurpose;
import dev.igorbarbosa.worktrainingsystem.files.domain.FileState;
import dev.igorbarbosa.worktrainingsystem.files.domain.UploadedFile;
import dev.igorbarbosa.worktrainingsystem.files.persistence.UploadedFileRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectMetadata;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorageException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService implements UploadedFileCatalog {
	private static final Map<FilePurpose, Map<String, Rule>> RULES = Map.of(
			FilePurpose.TRAINING_VIDEO, Map.of("video/mp4", new Rule("mp4", 2L * 1024 * 1024 * 1024),
					"video/webm", new Rule("webm", 2L * 1024 * 1024 * 1024)),
			FilePurpose.EMPLOYEE_PHOTO, Map.of("image/jpeg", new Rule("jpg", 5L * 1024 * 1024),
					"image/png", new Rule("png", 5L * 1024 * 1024), "image/webp", new Rule("webp", 5L * 1024 * 1024)),
			FilePurpose.EXTERNAL_CERTIFICATE, Map.of("application/pdf", new Rule("pdf", 20L * 1024 * 1024)),
			FilePurpose.GENERATED_CERTIFICATE, Map.of("application/pdf", new Rule("pdf", 20L * 1024 * 1024)));

	private final UploadedFileRepository files;
	private final ObjectStorage storage;
	private final CurrentUserProvider currentUser;
	private final AuthorizationService authorization;
	private final Clock clock;

	public FileService(UploadedFileRepository files, ObjectStorage storage, CurrentUserProvider currentUser,
			AuthorizationService authorization, Clock clock) {
		this.files = files; this.storage = storage; this.currentUser = currentUser;
		this.authorization = authorization; this.clock = clock;
	}

	@Transactional
	public UploadResponse request(UploadRequest request) {
		CurrentUser actor = currentUser.requireCurrentUser();
		UUID owner = authorizeRequest(actor, request.purpose(), request.ownerEmployeeId());
		String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
		Rule rule = RULES.get(request.purpose()).get(contentType);
		if (rule == null) throw violation("UPLOAD_TYPE_INVALID", "O tipo do arquivo não é permitido para essa finalidade.");
		if (request.sizeBytes() > rule.maxBytes()) throw violation("UPLOAD_TOO_LARGE", "O arquivo excede o tamanho permitido.");
		String objectKey = "organizations/%s/%s/%s.%s".formatted(DEFAULT_ORGANIZATION_ID,
				request.purpose().name().toLowerCase(Locale.ROOT), UUID.randomUUID(), rule.extension());
		var url = storage.presignUpload(objectKey);
		UploadedFile file = files.saveAndFlush(new UploadedFile(DEFAULT_ORGANIZATION_ID, request.purpose(), actor.userId(), owner,
				request.fileName().trim(), objectKey, contentType, request.sizeBytes(), normalizeChecksum(request.checksumSha256()), url.expiresAt()));
		String visibleKey = actor.role() == UserRole.ADMIN ? objectKey : null;
		Map<String, String> headers = file.getExpectedChecksumSha256() == null
				? Map.of("Content-Type", contentType)
				: Map.of("Content-Type", contentType, "X-Amz-Meta-Sha256", file.getExpectedChecksumSha256());
		return new UploadResponse(file.getId(), file.getId(), file.getPurpose(), file.getState(), "PUT",
				url.url(), visibleKey, url.expiresAt(), headers);
	}

	@Transactional(noRollbackFor = {BusinessRuleViolationException.class, ResourceConflictException.class})
	public UploadResponse complete(UUID id) {
		UploadedFile file = require(id);
		requireRequester(file);
		if (file.getState() == FileState.UPLOADED) return completedResponse(file);
		if (file.getState() != FileState.REQUESTED) throw conflict("UPLOAD_NOT_PENDING", "O upload não está pendente.");
		Instant now = clock.instant();
		if (!now.isBefore(file.getExpiresAt())) {
			file.expire(now); safeDelete(file.getObjectKey());
			throw conflict("UPLOAD_EXPIRED", "A solicitação de upload expirou.");
		}
		ObjectMetadata metadata;
		try { metadata = storage.head(file.getObjectKey()); }
		catch (ObjectStorageException exception) {
			file.fail("Objeto não encontrado no armazenamento.", now);
			throw violation("UPLOAD_OBJECT_NOT_FOUND", "O objeto enviado não foi encontrado.");
		}
		String mismatch = metadataMismatch(file, metadata);
		if (mismatch != null) {
			file.fail(mismatch, now); safeDelete(file.getObjectKey());
			throw violation("UPLOAD_METADATA_MISMATCH", mismatch);
		}
		file.complete(metadata.contentType(), metadata.contentLength(), normalizeChecksum(metadata.checksumSha256()), now);
		return completedResponse(file);
	}

	@Transactional
	public void cancel(UUID id) {
		UploadedFile file = require(id);
		CurrentUser actor = currentUser.requireCurrentUser();
		if (!file.getRequestedByUserId().equals(actor.userId()) && actor.role() != UserRole.ADMIN) throw denied();
		if (file.getState() == FileState.CANCELLED) return;
		if (file.getState() != FileState.REQUESTED) throw conflict("UPLOAD_NOT_PENDING", "O upload não está pendente.");
		file.cancel(clock.instant()); safeDelete(file.getObjectKey());
	}

	@Transactional(readOnly = true)
	public FileUrlResponse downloadUrl(UUID id) {
		UploadedFile file = requireUploaded(id);
		authorizeDownload(file);
		var url = storage.presignDownload(file.getObjectKey());
		return new FileUrlResponse(url.url(), url.expiresAt());
	}

	@Override @Transactional(readOnly = true)
	public FileReference requireTrainingVideo(UUID fileId) {
		return trainingVideoReference(requireUploaded(fileId));
	}

	@Override @Transactional(readOnly = true)
	public FileReference requireTrainingVideo(String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			throw violation("TRAINING_VIDEO_FILE_REQUIRED", "Informe a referência de um upload de vídeo concluído.");
		}
		UploadedFile file = files.findByOrganizationIdAndObjectKey(DEFAULT_ORGANIZATION_ID, objectKey.trim())
				.orElseThrow(() -> new ResourceNotFoundException("O arquivo de vídeo informado não existe."));
		if (file.getState() != FileState.UPLOADED) {
			throw conflict("FILE_NOT_AVAILABLE", "O arquivo de vídeo ainda não está disponível.");
		}
		return trainingVideoReference(file);
	}

	private FileReference trainingVideoReference(UploadedFile file) {
		if (file.getPurpose() != FilePurpose.TRAINING_VIDEO)
			throw violation("FILE_PURPOSE_INVALID", "O arquivo não é um vídeo de treinamento.");
		return new FileReference(file.getId(), file.getObjectKey(), file.getActualContentType(), file.getActualSizeBytes());
	}

	@Override @Transactional(readOnly = true)
	public FileReference requireExternalCertificate(UUID fileId, UUID employeeId) {
		UploadedFile file = requireUploaded(fileId);
		if (file.getPurpose() != FilePurpose.EXTERNAL_CERTIFICATE)
			throw violation("FILE_PURPOSE_INVALID", "O arquivo não é um certificado externo.");
		if (file.getOwnerEmployeeId() != null && !file.getOwnerEmployeeId().equals(employeeId))
			throw violation("FILE_OWNER_INVALID", "O certificado externo pertence a outro colaborador.");
		return new FileReference(file.getId(), file.getObjectKey(), file.getActualContentType(), file.getActualSizeBytes());
	}

	@Transactional
	public int expirePending() {
		Instant now = clock.instant(); int count = 0;
		for (UploadedFile file : files.findTop100ByStateAndExpiresAtBeforeOrderByExpiresAt(FileState.REQUESTED, now)) {
			file.expire(now); safeDelete(file.getObjectKey()); count++;
		}
		return count;
	}

	private UUID authorizeRequest(CurrentUser actor, FilePurpose purpose, UUID requestedOwner) {
		if (purpose == FilePurpose.TRAINING_VIDEO || purpose == FilePurpose.GENERATED_CERTIFICATE) {
			if (actor.role() != UserRole.ADMIN) throw denied();
			return requestedOwner;
		}
		if (actor.role() == UserRole.EMPLOYEE) {
			if (actor.employeeId() == null || requestedOwner != null && !actor.employeeId().equals(requestedOwner)) throw denied();
			return actor.employeeId();
		}
		if (actor.role() == UserRole.ADMIN && requestedOwner != null) return requestedOwner;
		throw denied();
	}

	private void authorizeDownload(UploadedFile file) {
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.role() == UserRole.ADMIN) return;
		if (file.getPurpose() == FilePurpose.TRAINING_VIDEO) throw denied();
		if (file.getOwnerEmployeeId() != null && authorization.canAccessEmployee(file.getOwnerEmployeeId())) return;
		throw denied();
	}

	private String metadataMismatch(UploadedFile file, ObjectMetadata metadata) {
		if (metadata.contentLength() != file.getExpectedSizeBytes()) return "O tamanho armazenado difere do tamanho solicitado.";
		if (metadata.contentType() == null || !file.getExpectedContentType().equalsIgnoreCase(metadata.contentType()))
			return "O tipo armazenado difere do tipo solicitado.";
		if (file.getExpectedChecksumSha256() != null && !file.getExpectedChecksumSha256().equals(normalizeChecksum(metadata.checksumSha256())))
			return "O checksum armazenado difere do checksum solicitado.";
		return null;
	}

	private UploadedFile require(UUID id) {
		return files.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O arquivo informado não existe."));
	}
	private UploadedFile requireUploaded(UUID id) {
		UploadedFile file = require(id);
		if (file.getState() != FileState.UPLOADED) throw conflict("FILE_NOT_AVAILABLE", "O arquivo ainda não está disponível.");
		return file;
	}
	private void requireRequester(UploadedFile file) {
		if (!file.getRequestedByUserId().equals(currentUser.requireCurrentUser().userId())) throw denied();
	}
	private UploadResponse completedResponse(UploadedFile file) {
		return new UploadResponse(file.getId(), file.getId(), file.getPurpose(), file.getState(), null, null, null,
				file.getExpiresAt(), Map.of());
	}
	private void safeDelete(String key) { try { storage.delete(key); } catch (RuntimeException ignored) { } }
	private String normalizeChecksum(String value) { return value == null || value.isBlank() ? null : value.toLowerCase(Locale.ROOT); }
	private AccessDeniedException denied() { return new AccessDeniedException("Arquivo fora do escopo autorizado."); }
	private BusinessRuleViolationException violation(String code, String message) { return new BusinessRuleViolationException(code, message); }
	private ResourceConflictException conflict(String code, String message) { return new ResourceConflictException(code, message); }
	private record Rule(String extension, long maxBytes) {}
}
