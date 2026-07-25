package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.domain.Permission;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
	private final JwtEncoder encoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
		this.encoder = encoder; this.properties = properties; this.clock = clock;
	}

	public String issue(User user, Set<Permission> permissions) {
		Instant issuedAt = clock.instant();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(properties.issuer()).audience(List.of(properties.audience()))
				.issuedAt(issuedAt).expiresAt(issuedAt.plus(properties.accessTokenTtl()))
				.subject(user.getId().toString()).claim("org", user.getOrganizationId().toString())
				.claim("email", user.getEmail()).claim("role", user.getRole().name())
				.claim("permissions", permissions.stream().map(Enum::name).sorted().toList());
		if (user.getEmployeeId() != null) claims.claim("employee_id", user.getEmployeeId().toString());
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
				.getTokenValue();
	}
}
