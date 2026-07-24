package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "trainings")
public class Training extends BaseEntity {

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(length = 2000)
	private String description;

	@Column(length = 150)
	private String category;

	@Column(name = "is_regulatory_standard", nullable = false)
	private boolean regulatoryStandard;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Training() {
	}

	public Training(
			UUID organizationId,
			String name,
			String code,
			String description,
			String category,
			boolean regulatoryStandard,
			RegistrationStatus status) {
		this.organizationId = organizationId;
		this.name = name;
		this.code = code;
		this.description = description;
		this.category = category;
		this.regulatoryStandard = regulatoryStandard;
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

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public boolean isRegulatoryStandard() {
		return regulatoryStandard;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(
			String name,
			String code,
			String description,
			String category,
			boolean regulatoryStandard) {
		this.name = name;
		this.code = code;
		this.description = description;
		this.category = category;
		this.regulatoryStandard = regulatoryStandard;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
}
