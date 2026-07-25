package dev.igorbarbosa.worktrainingsystem.activities.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;
	@Column(nullable = false, length = 150)
	private String name;
	@Column(length = 2000)
	private String description;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Activity() {}

	public Activity(UUID organizationId, String name, String description, RegistrationStatus status) {
		this.organizationId = organizationId;
		this.name = name;
		this.description = description;
		this.status = status;
	}

	public UUID getOrganizationId() { return organizationId; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public RegistrationStatus getStatus() { return status; }
	public void update(String name, String description) { this.name = name; this.description = description; }
	public void changeStatus(RegistrationStatus status) { this.status = status; }
}
