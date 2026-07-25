package dev.igorbarbosa.worktrainingsystem.certificates.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificateRevocationRequest(@NotBlank @Size(max = 1000) String reason) {}
