package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_permissions")
public class UserPermission extends BaseEntity {
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 64)
	private Permission permission;
	protected UserPermission() {}
	public UserPermission(UUID userId, Permission permission) { this.userId = userId; this.permission = permission; }
	public UUID getUserId() { return userId; }
	public Permission getPermission() { return permission; }
}
