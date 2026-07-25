package dev.igorbarbosa.worktrainingsystem.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordSecurityTest {
	private final PasswordPolicy policy = new PasswordPolicy();

	@Test
	void acceptsStrongPasswordAndBcryptStrengthTwelve() {
		policy.validate("ValidPassword1!");
		String hash = new BCryptPasswordEncoder(12).encode("ValidPassword1!");
		assertThat(hash).startsWith("$2a$12$");
		assertThat(new BCryptPasswordEncoder().matches("ValidPassword1!", hash)).isTrue();
	}

	@Test
	void rejectsPasswordMissingRequiredCharacterClasses() {
		assertThatThrownBy(() -> policy.validate("onlylowercase"))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasMessageContaining("12 caracteres");
	}
}
