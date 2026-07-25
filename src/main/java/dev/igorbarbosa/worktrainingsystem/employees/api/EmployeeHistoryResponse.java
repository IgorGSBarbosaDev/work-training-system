package dev.igorbarbosa.worktrainingsystem.employees.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistory;
import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistoryType;
import java.time.Instant;
import java.util.UUID;

public record EmployeeHistoryResponse(UUID id, EmployeeHistoryType changeType, UUID responsibleUserId,
		JsonNode before, JsonNode after, Instant createdAt) {
	public static EmployeeHistoryResponse from(EmployeeHistory history, JsonNode before, JsonNode after) {
		return new EmployeeHistoryResponse(history.getId(), history.getChangeType(), history.getResponsibleUserId(),
				before, after, history.getCreatedAt());
	}
}
