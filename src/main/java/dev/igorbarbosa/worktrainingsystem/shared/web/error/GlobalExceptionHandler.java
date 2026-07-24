package dev.igorbarbosa.worktrainingsystem.shared.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toApiFieldError)
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR",
				"Um ou mais campos são inválidos.", request, fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> handleConstraintViolation(
			ConstraintViolationException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getConstraintViolations().stream()
				.map(violation -> new ApiFieldError(
						violation.getPropertyPath().toString(),
						violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
						violation.getMessage()))
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR",
				"Um ou mais campos são inválidos.", request, fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadableMessage(HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "VALIDATION_ERROR",
				"O corpo da requisição é inválido.", request, List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		String requestId = requestId(request);
		LOGGER.error("Unexpected error. requestId={}", requestId, exception);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "INTERNAL_ERROR",
				"Ocorreu um erro interno.", request, requestId, List.of());
	}

	private ApiFieldError toApiFieldError(FieldError fieldError) {
		return new ApiFieldError(fieldError.getField(), fieldError.getCode(), fieldError.getDefaultMessage());
	}

	private ResponseEntity<ApiError> response(
			HttpStatus status,
			String error,
			String code,
			String message,
			HttpServletRequest request,
			List<ApiFieldError> fieldErrors) {
		return response(status, error, code, message, request, requestId(request), fieldErrors);
	}

	private ResponseEntity<ApiError> response(
			HttpStatus status,
			String error,
			String code,
			String message,
			HttpServletRequest request,
			String requestId,
			List<ApiFieldError> fieldErrors) {
		ApiError apiError = new ApiError(
				Instant.now(),
				status.value(),
				error,
				code,
				message,
				request.getRequestURI(),
				requestId,
				fieldErrors);
		return ResponseEntity.status(status).body(apiError);
	}

	private String requestId(HttpServletRequest request) {
		String suppliedRequestId = request.getHeader(REQUEST_ID_HEADER);
		return suppliedRequestId == null || suppliedRequestId.isBlank()
				? UUID.randomUUID().toString()
				: suppliedRequestId;
	}
}
