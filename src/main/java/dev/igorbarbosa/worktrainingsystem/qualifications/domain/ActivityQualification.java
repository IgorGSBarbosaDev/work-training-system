package dev.igorbarbosa.worktrainingsystem.qualifications.domain;

import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activity_qualifications")
public class ActivityQualification extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
	@Column(name = "activity_id", nullable = false, updatable = false) private UUID activityId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private QualificationStatus status;
	@Column(name = "calculated_at", nullable = false) private Instant calculatedAt;
	@Column(name = "next_expiration_date") private LocalDate nextExpirationDate;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "blocking_reasons", nullable = false, columnDefinition = "jsonb") private String blockingReasons;
	protected ActivityQualification() {}
	public UUID getOrganizationId() { return organizationId; }
	public UUID getEmployeeId() { return employeeId; }
	public UUID getActivityId() { return activityId; }
	public QualificationStatus getStatus() { return status; }
	public Instant getCalculatedAt() { return calculatedAt; }
	public LocalDate getNextExpirationDate() { return nextExpirationDate; }
	public String getBlockingReasons() { return blockingReasons; }
}
