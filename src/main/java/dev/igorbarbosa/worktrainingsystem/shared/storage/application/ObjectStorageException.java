package dev.igorbarbosa.worktrainingsystem.shared.storage.application;

public class ObjectStorageException extends RuntimeException {

	public ObjectStorageException(String operation, Throwable cause) {
		super("Object storage operation failed: " + operation, cause);
	}
}
