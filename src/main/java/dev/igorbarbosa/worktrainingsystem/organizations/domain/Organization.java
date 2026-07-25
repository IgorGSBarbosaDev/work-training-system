package dev.igorbarbosa.worktrainingsystem.organizations.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Organization() {
	}

	public String getName() {
		return name;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String name, RegistrationStatus status) {
		this.name = name;
		this.status = status;
	}
}
