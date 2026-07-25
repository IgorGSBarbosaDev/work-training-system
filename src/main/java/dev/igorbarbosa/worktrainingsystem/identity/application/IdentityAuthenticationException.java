package dev.igorbarbosa.worktrainingsystem.identity.application;

import org.springframework.http.HttpStatus;

public class IdentityAuthenticationException extends RuntimeException {
	private final String code;
	private final HttpStatus status;

	public IdentityAuthenticationException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public String getCode() { return code; }
	public HttpStatus getStatus() { return status; }

	public static IdentityAuthenticationException invalidCredentials() {
		return new IdentityAuthenticationException("INVALID_CREDENTIALS", "E-mail ou senha inválidos.", HttpStatus.UNAUTHORIZED);
	}

	public static IdentityAuthenticationException locked() {
		return new IdentityAuthenticationException("ACCOUNT_LOCKED", "Limite de tentativas atingido. Tente novamente mais tarde.", HttpStatus.TOO_MANY_REQUESTS);
	}

	public static IdentityAuthenticationException invalidToken() {
		return new IdentityAuthenticationException("INVALID_TOKEN", "Token inválido ou expirado.", HttpStatus.UNAUTHORIZED);
	}
}
