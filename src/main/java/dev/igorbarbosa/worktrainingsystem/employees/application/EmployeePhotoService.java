package dev.igorbarbosa.worktrainingsystem.employees.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeePhotoService {
	private static final long MAX_PHOTO_SIZE = 5L * 1024 * 1024;
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
	private final EmployeeService employees;
	private final ObjectStorage storage;

	public EmployeePhotoService(EmployeeService employees, ObjectStorage storage) {
		this.employees = employees;
		this.storage = storage;
	}

	public EmployeeResponse upload(UUID employeeId, MultipartFile file) {
		validate(file);
		employees.getById(employeeId);
		String contentType = file.getContentType();
		String key = "organizations/%s/employees/%s/photos/%s.%s".formatted(DEFAULT_ORGANIZATION_ID,
				employeeId, UUID.randomUUID(), EXTENSIONS.get(contentType));
		try {
			storage.upload(key, file.getInputStream(), file.getSize(), contentType);
		} catch (IOException exception) {
			throw new BusinessRuleViolationException("PHOTO_UPLOAD_FAILED", "Não foi possível ler a foto enviada.");
		}
		try {
			return employees.replacePhoto(employeeId, key, contentType, file.getSize());
		} catch (RuntimeException exception) {
			try { storage.delete(key); } catch (RuntimeException ignored) { exception.addSuppressed(ignored); }
			throw exception;
		}
	}

	public void delete(UUID employeeId) { employees.removePhoto(employeeId); }

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() <= 0) {
			throw new BusinessRuleViolationException("PHOTO_EMPTY", "A foto enviada está vazia.");
		}
		if (file.getSize() > MAX_PHOTO_SIZE) {
			throw new BusinessRuleViolationException("PHOTO_TOO_LARGE", "A foto deve possuir no máximo 5 MB.");
		}
		if (!EXTENSIONS.containsKey(file.getContentType())) {
			throw new BusinessRuleViolationException("PHOTO_TYPE_INVALID",
					"A foto deve estar no formato JPEG, PNG ou WEBP.");
		}
		try {
			byte[] header = file.getInputStream().readNBytes(12);
			if (!matchesSignature(file.getContentType(), header)) {
				throw new BusinessRuleViolationException("PHOTO_CONTENT_INVALID",
						"O conteúdo do arquivo não corresponde a uma imagem válida.");
			}
		} catch (IOException exception) {
			throw new BusinessRuleViolationException("PHOTO_UPLOAD_FAILED", "Não foi possível ler a foto enviada.");
		}
	}

	private boolean matchesSignature(String contentType, byte[] bytes) {
		return switch (contentType) {
			case "image/jpeg" -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
					&& (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
			case "image/png" -> bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
					&& bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
					&& bytes[6] == 0x1a && bytes[7] == 0x0a;
			case "image/webp" -> bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
					&& bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
					&& bytes[10] == 'B' && bytes[11] == 'P';
			default -> false;
		};
	}
}
