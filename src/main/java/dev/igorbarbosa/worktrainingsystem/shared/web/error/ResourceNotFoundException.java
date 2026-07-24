package dev.igorbarbosa.worktrainingsystem.shared.web.error;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
