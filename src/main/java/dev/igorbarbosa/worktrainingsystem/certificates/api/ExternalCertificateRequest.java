package dev.igorbarbosa.worktrainingsystem.certificates.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ExternalCertificateRequest(@NotNull UUID fileId) {}
