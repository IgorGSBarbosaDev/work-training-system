package dev.igorbarbosa.worktrainingsystem.organizations.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "units")
public class Unit extends BaseEntity {

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 20)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Unit() {
	}

	public Unit(UUID organizationId, String name, String code, RegistrationStatus status) {
		this.organizationId = organizationId;
		this.name = name;
		this.code = code;
		this.status = status;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
}
