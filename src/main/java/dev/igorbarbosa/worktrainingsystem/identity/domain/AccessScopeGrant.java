package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "access_scope_grants")
public class AccessScopeGrant extends BaseEntity {
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;
	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;
	@Enumerated(EnumType.STRING)
	@Column(name = "scope_type", nullable = false, updatable = false, length = 16)
	private ScopeType scopeType;
	@Column(name = "unit_id", updatable = false)
	private UUID unitId;
	@Column(name = "sector_id", updatable = false)
	private UUID sectorId;
	@Column(name = "employee_id", updatable = false)
	private UUID employeeId;
	@Column(nullable = false)
	private boolean active = true;

	protected AccessScopeGrant() {}
	public AccessScopeGrant(UUID userId, UUID organizationId, ScopeType type, UUID targetId) {
		this.userId = userId; this.organizationId = organizationId; this.scopeType = type;
		switch (type) { case UNIT -> unitId = targetId; case SECTOR -> sectorId = targetId; case EMPLOYEE -> employeeId = targetId; }
	}
	public UUID targetId() { return switch (scopeType) { case UNIT -> unitId; case SECTOR -> sectorId; case EMPLOYEE -> employeeId; }; }
	public UUID getUserId() { return userId; }
	public ScopeType getScopeType() { return scopeType; }
	public boolean isActive() { return active; }
}
