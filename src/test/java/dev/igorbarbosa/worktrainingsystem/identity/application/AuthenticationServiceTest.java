package dev.igorbarbosa.worktrainingsystem.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.LoginRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ChangePasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.api.IdentityDtos.ResetPasswordRequest;
import dev.igorbarbosa.worktrainingsystem.identity.config.IdentityProperties;
import dev.igorbarbosa.worktrainingsystem.identity.domain.PasswordResetToken;
import dev.igorbarbosa.worktrainingsystem.identity.domain.LoginAttemptState;
import dev.igorbarbosa.worktrainingsystem.identity.domain.RefreshToken;
import dev.igorbarbosa.worktrainingsystem.identity.domain.RefreshTokenFamily;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.PasswordResetTokenRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenFamilyRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.RefreshTokenRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserPermissionRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.LoginAttemptStateRepository;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthenticationServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
	private final UserRepository users = mock(UserRepository.class);
	private final UserPermissionRepository permissions = mock(UserPermissionRepository.class);
	private final RefreshTokenFamilyRepository families = mock(RefreshTokenFamilyRepository.class);
	private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
	private final PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
	private final LoginAttemptStateRepository loginAttempts = mock(LoginAttemptStateRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final JwtTokenService jwtTokens = mock(JwtTokenService.class);
	private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
	private final OpaqueTokenService opaqueTokens = new OpaqueTokenService();
	private AuthenticationService service;

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$secure-hash");
		service = new AuthenticationService(users, permissions, families, refreshTokens, resetTokens, loginAttempts,
				passwordEncoder, new PasswordPolicy(), opaqueTokens, jwtTokens,
				new JwtProperties("issuer", "audience", "01234567890123456789012345678901",
						Duration.ofMinutes(15), Duration.ofDays(7)),
				new IdentityProperties(5, Duration.ofMinutes(15), Duration.ofMinutes(15), Duration.ofMinutes(30),
						URI.create("http://localhost/reset-password"), 12),
				currentUserProvider, events, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void loginSucceedsAndStoresOnlyRefreshTokenHash() {
		User user = activeUser();
		when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("ValidPassword1!", user.getPasswordHash())).thenReturn(true);
		when(permissions.findAllByUserId(user.getId())).thenReturn(List.of());
		RefreshTokenFamily family = mock(RefreshTokenFamily.class);
		when(family.getId()).thenReturn(UUID.randomUUID());
		when(families.saveAndFlush(any())).thenReturn(family);
		when(jwtTokens.issue(any(), any())).thenReturn("access-token");

		var response = service.login(new LoginRequest("USER@example.com", "ValidPassword1!"));

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isNotBlank();
		ArgumentCaptor<RefreshToken> stored = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(stored.capture());
		assertThat(stored.getValue().getTokenHash()).isEqualTo(opaqueTokens.hash(response.refreshToken()));
		assertThat(stored.getValue().getTokenHash()).doesNotContain(response.refreshToken());
	}

	@Test
	void invalidPasswordIncrementsAttemptsAndFifthFailureLocksAccount() {
		User user = new User(UUID.randomUUID(), "user@example.com", "stored-password-hash",
				UserRole.EMPLOYEE, UserStatus.ACTIVE, UUID.randomUUID(), NOW);
		for (int attempt = 0; attempt < 4; attempt++) user.recordLoginFailure(5, NOW.plusSeconds(900));
		when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "wrong")))
				.isInstanceOfSatisfying(IdentityAuthenticationException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
		assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
		assertThat(user.getLockedUntil()).isEqualTo(NOW.plusSeconds(900));
	}

	@Test
	void unknownEmailStillPerformsPasswordHashComparison() {
		when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.com", "wrong")))
				.isInstanceOf(IdentityAuthenticationException.class);
		verify(passwordEncoder).matches(anyString(), anyString());
	}

	@Test
	void repeatedUnknownEmailAttemptsAreLockedUsingOnlyTheEmailHash() {
		String hash = opaqueTokens.hash("missing@example.com");
		LoginAttemptState state = new LoginAttemptState(hash, NOW);
		for (int attempt = 0; attempt < 4; attempt++) {
			state.recordFailure(NOW, Duration.ofMinutes(15), 5, Duration.ofMinutes(15));
		}
		when(loginAttempts.findByEmailHash(hash)).thenReturn(Optional.of(state));
		when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("MISSING@example.com", "wrong")))
				.isInstanceOfSatisfying(IdentityAuthenticationException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
		assertThat(state.getEmailHash()).isEqualTo(hash).doesNotContain("missing@example.com");
		assertThat(state.getFailedAttempts()).isEqualTo(5);
	}

	@Test
	void refreshRotatesFreshToken() {
		String value = "refresh-value";
		RefreshToken current = mock(RefreshToken.class);
		RefreshToken replacement = mock(RefreshToken.class);
		RefreshTokenFamily family = mock(RefreshTokenFamily.class);
		User user = activeUser();
		UUID userId = user.getId();
		UUID familyId = UUID.randomUUID();
		UUID replacementId = UUID.randomUUID();
		when(current.getFamilyId()).thenReturn(familyId);
		when(current.isFresh(NOW)).thenReturn(true);
		when(family.isUsable(NOW)).thenReturn(true);
		when(family.getUserId()).thenReturn(userId);
		when(family.getId()).thenReturn(familyId);
		when(family.getExpiresAt()).thenReturn(NOW.plus(Duration.ofDays(7)));
		when(refreshTokens.findByTokenHash(opaqueTokens.hash(value))).thenReturn(Optional.of(current));
		when(families.findById(familyId)).thenReturn(Optional.of(family));
		when(users.findById(userId)).thenReturn(Optional.of(user));
		when(refreshTokens.saveAndFlush(any())).thenReturn(replacement);
		when(replacement.getId()).thenReturn(replacementId);
		when(permissions.findAllByUserId(userId)).thenReturn(List.of());
		when(jwtTokens.issue(any(), any())).thenReturn("new-access");

		var response = service.refresh(value);

		verify(current).rotate(NOW, replacementId);
		assertThat(response.refreshToken()).isNotEqualTo(value);
	}

	@Test
	void refreshReuseRevokesWholeFamily() {
		RefreshToken reused = mock(RefreshToken.class);
		RefreshTokenFamily family = mock(RefreshTokenFamily.class);
		UUID familyId = UUID.randomUUID();
		when(reused.getFamilyId()).thenReturn(familyId);
		when(reused.getUsedAt()).thenReturn(NOW.minusSeconds(1));
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(reused));
		when(families.findById(familyId)).thenReturn(Optional.of(family));

		assertThatThrownBy(() -> service.refresh("reused-token"))
				.isInstanceOf(IdentityAuthenticationException.class);
		verify(family).revoke(NOW, "TOKEN_REUSE");
		verify(jwtTokens, never()).issue(any(), any());
	}

	@Test
	void resetConsumesTokenChangesHashAndRevokesSessions() {
		User user = activeUser();
		UUID userId = user.getId();
		PasswordResetToken token = mock(PasswordResetToken.class);
		RefreshTokenFamily family = mock(RefreshTokenFamily.class);
		when(resetTokens.findByTokenHash(opaqueTokens.hash("reset-value"))).thenReturn(Optional.of(token));
		when(token.isUsable(NOW)).thenReturn(true);
		when(token.getUserId()).thenReturn(userId);
		when(users.findById(userId)).thenReturn(Optional.of(user));
		when(families.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(family));
		when(resetTokens.findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId)).thenReturn(List.of());

		service.resetPassword(new ResetPasswordRequest("reset-value", "NewPassword1!"));

		verify(user).changePassword("$2a$12$secure-hash", NOW);
		verify(token).use(NOW);
		verify(family).revoke(NOW, "PASSWORD_RESET");
	}

	@Test
	void ownPasswordChangeRequiresCurrentPassword() {
		User user = activeUser();
		UUID userId = user.getId();
		UUID organizationId = user.getOrganizationId();
		when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(
				userId, organizationId, UserRole.EMPLOYEE,
				UUID.randomUUID(), java.util.Set.of()));
		when(users.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("CurrentPassword1!", user.getPasswordHash())).thenReturn(true);
		when(families.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of());

		service.changePassword(new ChangePasswordRequest("CurrentPassword1!", "NewPassword1!"));

		verify(user).changePassword("$2a$12$secure-hash", NOW);
	}

	private User activeUser() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(UUID.randomUUID());
		when(user.getOrganizationId()).thenReturn(UUID.randomUUID());
		when(user.getEmail()).thenReturn("user@example.com");
		when(user.getPasswordHash()).thenReturn("stored-password-hash");
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		return user;
	}
}
