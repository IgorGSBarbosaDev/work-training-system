package dev.igorbarbosa.worktrainingsystem.identity.config;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.identity.application.PasswordPolicy;
import dev.igorbarbosa.worktrainingsystem.identity.domain.User;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserStatus;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.UserRepository;
import java.time.Clock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
class DemoAdminBootstrap implements ApplicationRunner {
	private final DemoProperties properties;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;
	private final Clock clock;

	DemoAdminBootstrap(DemoProperties properties, UserRepository users, PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy, Clock clock) {
		this.properties = properties; this.users = users; this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy; this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (properties.adminEmail() == null || properties.adminEmail().isBlank()
				|| properties.adminPassword() == null || properties.adminPassword().isBlank()
				|| users.existsByEmailIgnoreCase(properties.adminEmail())) return;
		passwordPolicy.validate(properties.adminPassword());
		users.save(new User(DEFAULT_ORGANIZATION_ID, properties.adminEmail(),
				passwordEncoder.encode(properties.adminPassword()), UserRole.ADMIN, UserStatus.ACTIVE, null, clock.instant()));
	}
}
