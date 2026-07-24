package dev.igorbarbosa.worktrainingsystem.shared.web.pagination;

public class InvalidPaginationException extends RuntimeException {

	public InvalidPaginationException(String message) {
		super(message);
	}
}
