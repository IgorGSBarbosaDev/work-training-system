package dev.igorbarbosa.worktrainingsystem.shared.web.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".requestId";
	public static final String REQUEST_ID_MDC_KEY = "requestId";

	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
		String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (previousRequestId == null) {
				MDC.remove(REQUEST_ID_MDC_KEY);
			}
			else {
				MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
			}
		}
	}

	static String resolveRequestId(String suppliedRequestId) {
		return suppliedRequestId != null && SAFE_REQUEST_ID.matcher(suppliedRequestId).matches()
				? suppliedRequestId
				: newRequestId();
	}

	static String newRequestId() {
		return UUID.randomUUID().toString();
	}
}
