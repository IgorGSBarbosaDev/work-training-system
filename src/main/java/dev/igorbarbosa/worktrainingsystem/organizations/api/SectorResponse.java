package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record SectorResponse(
		UUID id,
		UUID unitId,
		String name,
		String code,
		RegistrationStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static SectorResponse from(Sector sector) {
		return new SectorResponse(
				sector.getId(),
				sector.getUnit().getId(),
				sector.getName(),
				sector.getCode(),
				sector.getStatus(),
				sector.getCreatedAt(),
				sector.getUpdatedAt());
	}
}
