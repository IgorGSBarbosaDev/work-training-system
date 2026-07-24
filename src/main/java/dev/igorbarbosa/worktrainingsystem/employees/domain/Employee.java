package dev.igorbarbosa.worktrainingsystem.employees.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 50)
	private String registration;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@Column(name = "sector_id", nullable = false)
	private UUID sectorId;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	@Column(name = "photo_url", length = 2048)
	private String photoUrl;

	protected Employee() {
	}

	public Employee(
			UUID organizationId,
			String name,
			String registration,
			String email,
			UUID jobId,
			UUID sectorId,
			UUID unitId,
			RegistrationStatus status) {
		this.organizationId = organizationId;
		this.name = name;
		this.registration = registration;
		this.email = email;
		this.jobId = jobId;
		this.sectorId = sectorId;
		this.unitId = unitId;
		this.status = status;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getName() {
		return name;
	}

	public String getRegistration() {
		return registration;
	}

	public String getEmail() {
		return email;
	}

	public UUID getJobId() {
		return jobId;
	}

	public UUID getSectorId() {
		return sectorId;
	}

	public UUID getUnitId() {
		return unitId;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void updateProfile(
			String name, String registration, String email, UUID sectorId, UUID unitId) {
		this.name = name;
		this.registration = registration;
		this.email = email;
		this.sectorId = sectorId;
		this.unitId = unitId;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}

	public void changeJob(UUID jobId) {
		this.jobId = jobId;
	}
}
