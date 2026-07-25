package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.AuthTokens;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ChangePasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.LoginRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ResetPasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.UserResponse;
import dev.igorbarbosa.worktrainingsystem.identity.config.IdentityProperties;
import dev.igorbarbosa.worktrainingsystem.identity.domain.PasswordResetToken;
import dev.igorbarbosa.worktrainingsystem.identity.domain.LoginAttemptState;
import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.RefreshToken;
import dev.igorbarbosa.worktrainingsystem.identity.domain.RefreshTokenFamily;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.PasswordResetTokenRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.LoginAttemptStateRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenFamilyRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserPermissionRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
	private final UserRepository users;
	private final UserPermissionRepository permissions;
	private final RefreshTokenFamilyRepository families;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordResetTokenRepository resetTokens;
	private final LoginAttemptStateRepository loginAttempts;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;
	private final OpaqueTokenService opaqueTokens;
	private final JwtTokenService jwtTokens;
	private final JwtProperties jwtProperties;
	private final IdentityProperties identityProperties;
	private final CurrentUserProvider currentUserProvider;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final String dummyPasswordHash;

	public AuthenticationService(UserRepository users, UserPermissionRepository permissions,
			RefreshTokenFamilyRepository families, RefreshTokenRepository refreshTokens,
			PasswordResetTokenRepository resetTokens, LoginAttemptStateRepository loginAttempts,
			PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy, OpaqueTokenService opaqueTokens, JwtTokenService jwtTokens,
			JwtProperties jwtProperties, IdentityProperties identityProperties,
			CurrentUserProvider currentUserProvider, ApplicationEventPublisher events, Clock clock) {
		this.users = users; this.permissions = permissions; this.families = families; this.refreshTokens = refreshTokens;
		this.resetTokens = resetTokens; this.loginAttempts = loginAttempts;
		this.passwordEncoder = passwordEncoder; this.passwordPolicy = passwordPolicy;
		this.opaqueTokens = opaqueTokens; this.jwtTokens = jwtTokens; this.jwtProperties = jwtProperties;
		this.identityProperties = identityProperties; this.currentUserProvider = currentUserProvider;
		this.events = events; this.clock = clock; this.dummyPasswordHash = passwordEncoder.encode(opaqueTokens.generate());
	}

	@Transactional(noRollbackFor = IdentityAuthenticationException.class)
	public AuthTokens login(LoginRequest request) {
		Instant now = clock.instant();
		String normalizedEmail = User.normalizeEmail(request.email());
		String emailHash = opaqueTokens.hash(normalizedEmail);
		LoginAttemptState attemptState = loginAttempts.findByEmailHash(emailHash).orElse(null);
		if (attemptState != null && attemptState.isLocked(now)) throw IdentityAuthenticationException.locked();
		User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			recordLoginFailure(emailHash, attemptState, now);
			throw IdentityAuthenticationException.invalidCredentials();
		}
		if (user.getStatus() == UserStatus.LOCKED || user.isTemporarilyLocked(now)) throw IdentityAuthenticationException.locked();
		if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			if (user.getStatus() == UserStatus.ACTIVE) {
				user.recordLoginFailure(identityProperties.loginFailureThreshold(), now.plus(identityProperties.loginLockDuration()));
			}
			recordLoginFailure(emailHash, attemptState, now);
			if (user.isTemporarilyLocked(now) || attemptState != null && attemptState.isLocked(now)) {
				throw IdentityAuthenticationException.locked();
			}
			throw IdentityAuthenticationException.invalidCredentials();
		}
		user.recordLoginSuccess();
		loginAttempts.deleteByEmailHash(emailHash);
		return issueTokenPair(user, null);
	}

	private void recordLoginFailure(String emailHash, LoginAttemptState existing, Instant now) {
		LoginAttemptState state = existing == null ? new LoginAttemptState(emailHash, now) : existing;
		state.recordFailure(now, identityProperties.loginAttemptWindow(), identityProperties.loginFailureThreshold(),
				identityProperties.loginLockDuration());
		loginAttempts.save(state);
		if (state.isLocked(now)) throw IdentityAuthenticationException.locked();
	}

	@Transactional(noRollbackFor = IdentityAuthenticationException.class)
	public AuthTokens refresh(String opaqueToken) {
		Instant now = clock.instant();
		RefreshToken current = refreshTokens.findByTokenHash(opaqueTokens.hash(opaqueToken))
				.orElseThrow(IdentityAuthenticationException::invalidToken);
		RefreshTokenFamily family = families.findById(current.getFamilyId())
				.orElseThrow(IdentityAuthenticationException::invalidToken);
		if (current.getUsedAt() != null || current.getRevokedAt() != null) {
			family.revoke(now, "TOKEN_REUSE");
			throw IdentityAuthenticationException.invalidToken();
		}
		if (!current.isFresh(now) || !family.isUsable(now)) throw IdentityAuthenticationException.invalidToken();
		User user = activeUser(family.getUserId());
		String replacementValue = opaqueTokens.generate();
		RefreshToken replacement = refreshTokens.saveAndFlush(new RefreshToken(
				family.getId(), opaqueTokens.hash(replacementValue), family.getExpiresAt()));
		current.rotate(now, replacement.getId());
		return issueTokenPair(user, replacementValue);
	}

	@Transactional
	public void logout(String opaqueToken) {
		refreshTokens.findByTokenHash(opaqueTokens.hash(opaqueToken)).ifPresent(token -> token.revoke(clock.instant()));
	}

	@Transactional(readOnly = true)
	public UserResponse me() {
		return response(users.findById(currentUserProvider.requireCurrentUser().userId())
				.orElseThrow(IdentityAuthenticationException::invalidToken));
	}

	@Transactional
	public void forgotPassword(String email) {
		users.findByEmailIgnoreCase(User.normalizeEmail(email)).filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.ifPresent(this::createResetToken);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		passwordPolicy.validate(request.newPassword());
		PasswordResetToken token = resetTokens.findByTokenHash(opaqueTokens.hash(request.token()))
				.orElseThrow(IdentityAuthenticationException::invalidToken);
		Instant now = clock.instant();
		if (!token.isUsable(now)) throw IdentityAuthenticationException.invalidToken();
		User user = activeUser(token.getUserId());
		user.changePassword(passwordEncoder.encode(request.newPassword()), now);
		token.use(now);
		revokeSessions(user.getId(), "PASSWORD_RESET");
		revokeOtherResetTokens(user.getId(), now);
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		passwordPolicy.validate(request.newPassword());
		User user = activeUser(currentUserProvider.requireCurrentUser().userId());
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw IdentityAuthenticationException.invalidCredentials();
		}
		user.changePassword(passwordEncoder.encode(request.newPassword()), clock.instant());
		revokeSessions(user.getId(), "PASSWORD_CHANGED");
	}

	@Transactional
	public void requestResetFor(User user) { createResetToken(user); }

	@Transactional
	public void revokeAllSessions(UUID userId, String reason) { revokeSessions(userId, reason); }

	private void createResetToken(User user) {
		Instant now = clock.instant();
		revokeOtherResetTokens(user.getId(), now);
		String value = opaqueTokens.generate();
		resetTokens.save(new PasswordResetToken(user.getId(), opaqueTokens.hash(value),
				now.plus(identityProperties.passwordResetTokenTtl())));
		events.publishEvent(new PasswordResetRequested(user.getEmail(), value));
	}

	private AuthTokens issueTokenPair(User user, String existingRefreshValue) {
		Set<Permission> effectivePermissions = effectivePermissions(user);
		String refreshValue = existingRefreshValue;
		if (refreshValue == null) {
			refreshValue = opaqueTokens.generate();
			Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
			RefreshTokenFamily family = families.saveAndFlush(new RefreshTokenFamily(user.getId(), expiresAt));
			refreshTokens.save(new RefreshToken(family.getId(), opaqueTokens.hash(refreshValue), expiresAt));
		}
		return new AuthTokens(jwtTokens.issue(user, effectivePermissions), refreshValue, "Bearer",
				jwtProperties.accessTokenTtl().toSeconds(), response(user, effectivePermissions));
	}

	private Set<Permission> effectivePermissions(User user) {
		if (user.getRole() == UserRole.ADMIN) return EnumSet.allOf(Permission.class);
		return permissions.findAllByUserId(user.getId()).stream().map(item -> item.getPermission())
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private User activeUser(UUID id) {
		User user = users.findById(id).orElseThrow(IdentityAuthenticationException::invalidToken);
		if (user.getStatus() != UserStatus.ACTIVE) throw IdentityAuthenticationException.invalidToken();
		return user;
	}

	private void revokeSessions(UUID userId, String reason) {
		Instant now = clock.instant();
		families.findAllByUserIdAndRevokedAtIsNull(userId).forEach(family -> family.revoke(now, reason));
	}

	private void revokeOtherResetTokens(UUID userId, Instant now) {
		resetTokens.findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
	}

	private UserResponse response(User user) { return response(user, effectivePermissions(user)); }
	private UserResponse response(User user, Set<Permission> effectivePermissions) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getEmployeeId(),
				effectivePermissions, user.getCreatedAt(), user.getUpdatedAt());
	}
}
