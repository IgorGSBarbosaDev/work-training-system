package dev.igorbarbosa.worktrainingsystem.certificates.api;

import java.net.URI;
import java.time.Instant;

public record CertificateDownloadResponse(URI url, Instant expiresAt) {}
