package dev.igorbarbosa.worktrainingsystem.progress.api;

import java.net.URI;
import java.time.Instant;

public record PlaybackUrlResponse(URI url, Instant expiresAt, long resumeAtSeconds) {
}
