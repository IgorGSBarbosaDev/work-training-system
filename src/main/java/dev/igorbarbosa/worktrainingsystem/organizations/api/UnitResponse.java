package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record UnitResponse(
		UUID id,
		String name,
		String code,
		RegistrationStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static UnitResponse from(Unit unit) {
		return new UnitResponse(
				unit.getId(),
				unit.getName(),
				unit.getCode(),
				unit.getStatus(),
				unit.getCreatedAt(),
				unit.getUpdatedAt());
	}
}
