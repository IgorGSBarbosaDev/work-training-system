package dev.igorbarbosa.worktrainingsystem.identity.api;

import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.ScopeType;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class IdentityDtos {
	private IdentityDtos() {}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
	public record RefreshRequest(@NotBlank String refreshToken) {}
	public record LogoutRequest(@NotBlank String refreshToken) {}
	public record ForgotPasswordRequest(@NotBlank @Email String email) {}
	public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}
	public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
	public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresIn,
			UserResponse user) {}
	public record UserResponse(UUID id, String email, UserRole role, UserStatus status, UUID employeeId,
			Set<Permission> permissions, Instant createdAt, Instant updatedAt) {}
	public record CreateUserRequest(@NotBlank @Email String email, @NotNull UserRole role, UUID employeeId,
			boolean sendActivationEmail) {}
	public record UpdateUserRequest(@NotBlank @Email String email, @NotNull UserRole role, UUID employeeId) {}
	public record ChangeUserStatusRequest(@NotNull UserStatus status) {}
	public record ScopeGrantRequest(@NotNull ScopeType type, @NotNull UUID targetId) {}
	public record UpdatePermissionsRequest(@NotNull Set<Permission> permissions,
			@NotNull List<@Valid ScopeGrantRequest> scopes) {}
	public record PermissionsResponse(Set<Permission> permissions, List<ScopeGrantRequest> scopes) {}
}
