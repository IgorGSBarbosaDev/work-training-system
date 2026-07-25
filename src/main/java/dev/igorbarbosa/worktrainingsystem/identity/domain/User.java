package dev.igorbarbosa.worktrainingsystem.identity.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserStatus status;

	@Column(name = "employee_id")
	private UUID employeeId;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "password_changed_at", nullable = false)
	private Instant passwordChangedAt;

	protected User() {
	}

	public User(UUID organizationId, String email, String passwordHash, UserRole role, UserStatus status,
			UUID employeeId, Instant now) {
		this.organizationId = organizationId;
		this.email = normalizeEmail(email);
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
		this.employeeId = employeeId;
		this.passwordChangedAt = now;
	}

	public static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	public void recordLoginFailure(int threshold, Instant lockedUntil) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= threshold) {
			this.lockedUntil = lockedUntil;
		}
	}

	public void recordLoginSuccess() {
		failedLoginAttempts = 0;
		lockedUntil = null;
	}

	public boolean isTemporarilyLocked(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	public void changePassword(String passwordHash, Instant now) {
		this.passwordHash = passwordHash;
		this.passwordChangedAt = now;
		recordLoginSuccess();
	}

	public void update(String email, UserRole role, UUID employeeId) {
		this.email = normalizeEmail(email);
		this.role = role;
		this.employeeId = employeeId;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
		if (status == UserStatus.ACTIVE) recordLoginSuccess();
		else if (status != UserStatus.LOCKED) this.lockedUntil = null;
	}

	public UUID getOrganizationId() { return organizationId; }
	public String getEmail() { return email; }
	public String getPasswordHash() { return passwordHash; }
	public UserRole getRole() { return role; }
	public UserStatus getStatus() { return status; }
	public UUID getEmployeeId() { return employeeId; }
	public int getFailedLoginAttempts() { return failedLoginAttempts; }
	public Instant getLockedUntil() { return lockedUntil; }
	public Instant getPasswordChangedAt() { return passwordChangedAt; }
}
