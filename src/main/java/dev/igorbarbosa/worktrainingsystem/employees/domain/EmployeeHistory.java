package dev.igorbarbosa.worktrainingsystem.employees.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "employee_history")
@EntityListeners(AuditingEntityListener.class)
public class EmployeeHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false)
	private UUID employeeId;
	@Enumerated(EnumType.STRING)
	@Column(name = "change_type", nullable = false, updatable = false, length = 32)
	private EmployeeHistoryType changeType;
	@Column(name = "responsible_user_id", nullable = false, updatable = false)
	private UUID responsibleUserId;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_state", columnDefinition = "jsonb", updatable = false)
	private JsonNode beforeState;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_state", columnDefinition = "jsonb", updatable = false)
	private JsonNode afterState;
	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected EmployeeHistory() {}

	public EmployeeHistory(UUID organizationId, UUID employeeId, EmployeeHistoryType changeType,
			UUID responsibleUserId, JsonNode beforeState, JsonNode afterState) {
		this.organizationId = organizationId;
		this.employeeId = employeeId;
		this.changeType = changeType;
		this.responsibleUserId = responsibleUserId;
		this.beforeState = beforeState;
		this.afterState = afterState;
	}

	public UUID getId() { return id; }
	public UUID getEmployeeId() { return employeeId; }
	public EmployeeHistoryType getChangeType() { return changeType; }
	public UUID getResponsibleUserId() { return responsibleUserId; }
	public JsonNode getBeforeState() { return beforeState; }
	public JsonNode getAfterState() { return afterState; }
	public Instant getCreatedAt() { return createdAt; }
}
