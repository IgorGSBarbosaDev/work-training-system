package dev.igorbarbosa.worktrainingsystem.files.api;

import java.net.URI;
import java.time.Instant;

public record FileUrlResponse(URI url, Instant expiresAt) {
}
