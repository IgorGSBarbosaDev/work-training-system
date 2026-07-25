package dev.igorbarbosa.worktrainingsystem.shared.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.InvalidPaginationException;
import dev.igorbarbosa.worktrainingsystem.identity.application.IdentityAuthenticationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
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

	@ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
	ResponseEntity<ApiError> handleRequestParameterValidation(HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR",
				"Um ou mais parâmetros são inválidos.", request, List.of());
	}

	@ExceptionHandler(InvalidPaginationException.class)
	ResponseEntity<ApiError> handleInvalidPagination(
			InvalidPaginationException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR",
				exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> handleResourceNotFound(
			ResourceNotFoundException exception, HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "RESOURCE_NOT_FOUND",
				exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(IdentityAuthenticationException.class)
	ResponseEntity<ApiError> handleIdentityAuthentication(
			IdentityAuthenticationException exception, HttpServletRequest request) {
		return response(exception.getStatus(), exception.getStatus().getReasonPhrase().toUpperCase().replace(' ', '_'),
				exception.getCode(), exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(ResourceConflictException.class)
	ResponseEntity<ApiError> handleResourceConflict(
			ResourceConflictException exception, HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", exception.getCode(),
				exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(BusinessRuleViolationException.class)
	ResponseEntity<ApiError> handleBusinessRuleViolation(
			BusinessRuleViolationException exception, HttpServletRequest request) {
		return response(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION", exception.getCode(),
				exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleDataIntegrityViolation(HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", "RESOURCE_ALREADY_EXISTS",
				"Já existe um cadastro com os dados informados.", request, List.of());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ApiError> handleOptimisticLockingFailure(HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", "CONCURRENT_MODIFICATION",
				"O cadastro foi alterado por outra requisição. Recarregue os dados e tente novamente.",
				request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
		return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "ACCESS_DENIED",
				"Você não possui permissão para executar esta operação.", request, List.of());
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
		return ResponseEntity.status(status)
				.header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId)
				.body(apiError);
	}

	private String requestId(HttpServletRequest request) {
		Object requestId = request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
		return requestId instanceof String value
				? value
				: RequestCorrelationFilter.newRequestId();
	}
}
