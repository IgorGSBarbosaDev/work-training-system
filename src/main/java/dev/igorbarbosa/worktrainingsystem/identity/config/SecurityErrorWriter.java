package dev.igorbarbosa.worktrainingsystem.identity.config;

import dev.igorbarbosa.worktrainingsystem.shared.web.error.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
class SecurityErrorWriter {
	void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		write(request, response, 401, "UNAUTHORIZED", "INVALID_TOKEN", "Autenticação necessária ou token inválido.");
	}

	void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
		write(request, response, 403, "ACCESS_DENIED", "ACCESS_DENIED", "Você não possui permissão para executar esta operação.");
	}

	private void write(HttpServletRequest request, HttpServletResponse response, int status,
			String error, String code, String message) throws IOException {
		Object value = request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
		String requestId = value instanceof String id ? id : "unavailable";
		response.setStatus(status);
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId);
		response.getWriter().write("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"code\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"requestId\":\"%s\",\"fieldErrors\":[]}".formatted(
				Instant.now(), status, error, code, message, escape(request.getRequestURI()), escape(requestId)));
	}

	private String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
