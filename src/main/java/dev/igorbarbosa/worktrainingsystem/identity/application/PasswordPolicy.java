package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
	public void validate(String password) {
		boolean valid = password != null && password.length() >= 12
				&& password.chars().anyMatch(Character::isUpperCase)
				&& password.chars().anyMatch(Character::isLowerCase)
				&& password.chars().anyMatch(Character::isDigit)
				&& password.chars().anyMatch(value -> !Character.isLetterOrDigit(value));
		if (!valid) {
			throw new BusinessRuleViolationException("PASSWORD_POLICY_VIOLATION",
					"A senha deve possuir ao menos 12 caracteres, maiúscula, minúscula, número e caractere especial.");
		}
	}
}
