package dev.igorbarbosa.worktrainingsystem.employees.api;

import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
		UUID id,
		String name,
		String registration,
		String email,
		RegistrationStatus status,
		String photoUrl,
		Reference job,
		Reference sector,
		Reference unit,
		Instant createdAt,
		Instant updatedAt) {

	public static EmployeeResponse from(
			Employee employee, JobResponse job, SectorResponse sector, UnitResponse unit, String photoUrl) {
		return new EmployeeResponse(
				employee.getId(),
				employee.getName(),
				employee.getRegistration(),
				employee.getEmail(),
				employee.getStatus(),
				photoUrl,
				new Reference(job.id(), job.name()),
				new Reference(sector.id(), sector.name()),
				new Reference(unit.id(), unit.name()),
				employee.getCreatedAt(),
				employee.getUpdatedAt());
	}

	public record Reference(UUID id, String name) {
	}
}
