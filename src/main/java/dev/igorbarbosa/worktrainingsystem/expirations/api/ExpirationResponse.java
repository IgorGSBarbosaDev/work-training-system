package dev.igorbarbosa.worktrainingsystem.expirations.api;

import dev.igorbarbosa.worktrainingsystem.expirations.domain.ExpirationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ExpirationResponse(UUID completionId, UUID employeeId, UUID trainingId,
		LocalDate completionDate, LocalDate expirationDate, ExpirationStatus status) {}
