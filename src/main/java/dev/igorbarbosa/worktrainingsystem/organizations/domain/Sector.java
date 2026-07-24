package dev.igorbarbosa.worktrainingsystem.organizations.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sectors")
public class Sector extends BaseEntity {

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false, updatable = false)
	private Unit unit;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 20)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Sector() {
	}

	public Sector(UUID organizationId, Unit unit, String name, String code, RegistrationStatus status) {
		this.organizationId = organizationId;
		this.unit = unit;
		this.name = name;
		this.code = code;
		this.status = status;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public Unit getUnit() {
		return unit;
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
}
