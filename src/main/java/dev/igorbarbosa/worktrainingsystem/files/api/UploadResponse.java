package dev.igorbarbosa.worktrainingsystem.files.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.igorbarbosa.worktrainingsystem.files.domain.FilePurpose;
import dev.igorbarbosa.worktrainingsystem.files.domain.FileState;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UploadResponse(UUID uploadId, UUID fileId, FilePurpose purpose, FileState state, String method,
		URI uploadUrl, String objectKey, Instant expiresAt, Map<String, String> requiredHeaders) {
}
