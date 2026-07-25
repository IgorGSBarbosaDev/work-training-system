package dev.igorbarbosa.worktrainingsystem.files.api;

import dev.igorbarbosa.worktrainingsystem.files.domain.FilePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UploadRequest(
		@NotNull FilePurpose purpose,
		@NotBlank @Size(max = 255) String fileName,
		@NotBlank @Size(max = 100) String contentType,
		@Positive long sizeBytes,
		@Pattern(regexp = "[0-9a-fA-F]{64}", message = "O checksum SHA-256 deve possuir 64 caracteres hexadecimais.")
		String checksumSha256,
		UUID ownerEmployeeId) {
}
