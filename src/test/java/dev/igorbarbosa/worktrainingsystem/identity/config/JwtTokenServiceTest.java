package dev.igorbarbosa.worktrainingsystem.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.identity.application.JwtTokenService;
import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtTokenServiceTest {
	@Test
	void issuesAndValidatesIdentityRolePermissionIssuerAudienceAndExpiryClaims() {
		JwtProperties properties = new JwtProperties("test-issuer", "test-api",
				"01234567890123456789012345678901", Duration.ofMinutes(15), Duration.ofDays(7));
		SecurityConfiguration configuration = new SecurityConfiguration();
		var key = configuration.jwtSecretKey(properties);
		var encoder = configuration.jwtEncoder(key);
		JwtDecoder decoder = configuration.jwtDecoder(key, properties);
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		User user = mock(User.class);
		UUID userId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		when(user.getId()).thenReturn(userId);
		when(user.getOrganizationId()).thenReturn(organizationId);
		when(user.getEmployeeId()).thenReturn(employeeId);
		when(user.getEmail()).thenReturn("manager@example.com");
		when(user.getRole()).thenReturn(UserRole.MANAGER);
		String token = new JwtTokenService(encoder, properties, Clock.fixed(now, ZoneOffset.UTC))
				.issue(user, Set.of(Permission.ASSIGN_TRAINING));

		var jwt = decoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getAudience()).containsExactly("test-api");
		assertThat(jwt.getClaimAsString("org")).isEqualTo(organizationId.toString());
		assertThat(jwt.getClaimAsString("role")).isEqualTo("MANAGER");
		assertThat(jwt.getClaimAsStringList("permissions")).containsExactly("ASSIGN_TRAINING");
		assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
	}

	@Test
	void rejectsSigningSecretsShorterThanThirtyTwoBytes() {
		JwtProperties properties = new JwtProperties("issuer", "audience", "too-short",
				Duration.ofMinutes(15), Duration.ofDays(7));
		assertThatThrownBy(properties::signingKey).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32");
	}

	@Test
	void rejectsWrongAudienceAndExpiredTokens() {
		JwtProperties accepted = new JwtProperties("test-issuer", "test-api",
				"01234567890123456789012345678901", Duration.ofMinutes(15), Duration.ofDays(7));
		SecurityConfiguration configuration = new SecurityConfiguration();
		var key = configuration.jwtSecretKey(accepted);
		var encoder = configuration.jwtEncoder(key);
		JwtDecoder decoder = configuration.jwtDecoder(key, accepted);
		User user = mock(User.class);
		when(user.getId()).thenReturn(UUID.randomUUID());
		when(user.getOrganizationId()).thenReturn(UUID.randomUUID());
		when(user.getEmail()).thenReturn("employee@example.com");
		when(user.getRole()).thenReturn(UserRole.EMPLOYEE);

		JwtProperties wrongAudience = new JwtProperties("test-issuer", "other-api", accepted.signingSecret(),
				Duration.ofMinutes(15), Duration.ofDays(7));
		String audienceToken = new JwtTokenService(encoder, wrongAudience, Clock.systemUTC()).issue(user, Set.of());
		assertThatThrownBy(() -> decoder.decode(audienceToken)).isInstanceOf(JwtValidationException.class);

		String expiredToken = new JwtTokenService(encoder, accepted,
				Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC)).issue(user, Set.of());
		assertThatThrownBy(() -> decoder.decode(expiredToken)).isInstanceOf(JwtValidationException.class);
	}
}
