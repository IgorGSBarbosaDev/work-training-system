package dev.igorbarbosa.worktrainingsystem.employees.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistory;
import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistoryType;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmployeeHistoryRecorder {
	private final EmployeeHistoryRepository histories;
	private final CurrentUserProvider currentUsers;
	private final ObjectMapper objectMapper;

	public EmployeeHistoryRecorder(EmployeeHistoryRepository histories, CurrentUserProvider currentUsers,
			ObjectMapper objectMapper) {
		this.histories = histories;
		this.currentUsers = currentUsers;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> snapshot(Employee employee) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("name", employee.getName());
		state.put("registration", employee.getRegistration());
		state.put("email", employee.getEmail());
		state.put("status", employee.getStatus().name());
		state.put("jobId", employee.getJobId());
		state.put("sectorId", employee.getSectorId());
		state.put("unitId", employee.getUnitId());
		state.put("photoObjectKey", employee.getPhotoObjectKey());
		state.put("photoContentType", employee.getPhotoContentType());
		state.put("photoSizeBytes", employee.getPhotoSizeBytes());
		return state;
	}

	public void record(Employee employee, EmployeeHistoryType type, Map<String, Object> before) {
		histories.save(new EmployeeHistory(employee.getOrganizationId(), employee.getId(), type,
				currentUsers.requireCurrentUser().userId(), json(before), json(snapshot(employee))));
	}

	private JsonNode json(Map<String, Object> value) {
		if (value == null) return null;
		return objectMapper.valueToTree(value);
	}
}
