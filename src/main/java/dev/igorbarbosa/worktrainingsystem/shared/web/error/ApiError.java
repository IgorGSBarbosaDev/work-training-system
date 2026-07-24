package dev.igorbarbosa.worktrainingsystem.shared.web.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String code,
		String message,
		String path,
		String requestId,
		List<ApiFieldError> fieldErrors) {
}
