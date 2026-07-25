package dev.igorbarbosa.worktrainingsystem.shared.storage.application;

import java.net.URI;
import java.time.Instant;

public record PresignedObjectUrl(URI url, Instant expiresAt) {
}
