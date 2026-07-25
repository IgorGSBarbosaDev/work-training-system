package dev.igorbarbosa.worktrainingsystem.shared.storage.application;

import java.time.Instant;

public record ObjectMetadata(long contentLength, String contentType, String checksumSha256,
		String entityTag, Instant lastModified) {
}
