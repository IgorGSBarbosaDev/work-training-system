package dev.igorbarbosa.worktrainingsystem.identity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.identity.domain.AuditLog;
import dev.igorbarbosa.worktrainingsystem.identity.persistence.AuditLogRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.RequestCorrelationFilter;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PersistentAuditAdapter implements AuditPort {
	private final AuditLogRepository logs;
	private final ObjectMapper mapper;
	public PersistentAuditAdapter(AuditLogRepository logs, ObjectMapper mapper) {
		this.logs = logs; this.mapper = mapper;
	}
	@Override
	public void record(AuditRecord record) {
		try {
			String details = mapper.writeValueAsString(record.details() == null ? Map.of() : record.details());
			Object requestId = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() == null
					? null : ((org.springframework.web.context.request.ServletRequestAttributes)
						org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest()
						.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
			logs.save(new AuditLog(record,
					requestId == null ? null : requestId.toString(), details));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not serialize audit details", exception);
		}
	}
}
