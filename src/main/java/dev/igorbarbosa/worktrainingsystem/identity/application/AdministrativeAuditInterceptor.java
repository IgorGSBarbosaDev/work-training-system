package dev.igorbarbosa.worktrainingsystem.identity.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnBean({CurrentUserProvider.class, AuditPort.class})
public class AdministrativeAuditInterceptor implements HandlerInterceptor {
	private static final Logger LOG = LoggerFactory.getLogger(AdministrativeAuditInterceptor.class);
	private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
	private final CurrentUserProvider currentUser;
	private final AuditPort audit;
	private final Clock clock;

	public AdministrativeAuditInterceptor(CurrentUserProvider currentUser, AuditPort audit, Clock clock) {
		this.currentUser = currentUser; this.audit = audit; this.clock = clock;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception exception) {
		if (exception != null || response.getStatus() >= 400 || !isMutation(request.getMethod())) return;
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) return;
		String template = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
		String action = action(request.getMethod(), template);
		if (action == null) return;
		try {
			CurrentUser actor = currentUser.requireCurrentUser();
			audit.record(new AuditPort.AuditRecord(actor.organizationId(), actor.userId(), action,
					entityType(template), entityId(request.getRequestURI()), clock.instant(),
					Map.of("method", request.getMethod(), "route", template)));
		} catch (RuntimeException auditFailure) {
			// A auditoria complementar nunca altera a resposta já concluída.
			LOG.warn("Could not persist administrative audit for route {}", template, auditFailure);
		}
	}

	private boolean isMutation(String method) { return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method); }

	private String action(String method, String route) {
		if (route == null || "null".equals(route) || route.startsWith("/api/v1/auth") || route.startsWith("/api/v1/files")) return null;
		if (route.startsWith("/api/v1/users") || route.startsWith("/api/v1/certificates") || route.contains("/qr-code")) return null;
		String operation = "POST".equals(method) && !route.matches(".*\\{[^}]+}.*") ? "CREATED" : "UPDATED";
		if (route.contains("/job")) operation = "EMPLOYEE_JOB_CHANGED";
		else if (route.contains("/requirements")) operation = "ACTIVITY_REQUIREMENT_CHANGED";
		else if (route.contains("manual") || route.contains("training-completions")) operation = "MANUAL_COMPLETION_RECORDED";
		else if (route.startsWith("/api/v1/training-assignments") || route.startsWith("/api/v1/assignment-batches")) operation = "TRAINING_ASSIGNMENT_CHANGED";
		else if (route.contains("/publish") || route.contains("/versions")) operation = "TRAINING_VERSION_CHANGED";
		else operation = entityType(route) + "_" + operation;
		return operation;
	}

	private String entityType(String route) {
		if (route.contains("employees")) return "EMPLOYEE";
		if (route.contains("units")) return "UNIT";
		if (route.contains("sectors")) return "SECTOR";
		if (route.contains("jobs")) return "JOB";
		if (route.contains("activities")) return "ACTIVITY";
		if (route.contains("trainings") || route.contains("modules") || route.contains("question")) return "TRAINING";
		if (route.contains("assignments")) return "TRAINING_ASSIGNMENT";
		if (route.contains("completion")) return "TRAINING_COMPLETION";
		if (route.contains("organization")) return "ORGANIZATION";
		return "ADMIN_RESOURCE";
	}

	private UUID entityId(String uri) {
		var matcher = UUID_PATTERN.matcher(uri);
		return matcher.find() ? UUID.fromString(matcher.group()) : null;
	}
}
