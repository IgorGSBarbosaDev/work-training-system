package dev.igorbarbosa.worktrainingsystem.qrverification.api;

import java.time.Instant;
import java.util.UUID;

public record QrAccessLogResponse(UUID id, UUID qrCodeId, UUID queriedByUserId, Instant queriedAt, String result, String requestId) {}
