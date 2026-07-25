package dev.igorbarbosa.worktrainingsystem.qrverification.api;

import dev.igorbarbosa.worktrainingsystem.qrverification.domain.EmployeeQrCode.Status;
import java.time.Instant;
import java.util.UUID;

public record QrCodeResponse(UUID id, UUID employeeId, String token, Status status, Instant generatedAt, Instant revokedAt, String revocationReason) {}
