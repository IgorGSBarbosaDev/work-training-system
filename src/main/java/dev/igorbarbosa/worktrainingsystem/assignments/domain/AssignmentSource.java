package dev.igorbarbosa.worktrainingsystem.assignments.domain;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "assignment_sources")
@EntityListeners(AuditingEntityListener.class)
public class AssignmentSource {
	@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
	@Column(name = "organization_id", nullable = false, updatable = false) private UUID organizationId;
	@Column(name = "assignment_id", nullable = false, updatable = false) private UUID assignmentId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 32) private AssignmentOrigin origin;
	@Column(name = "source_reference_id", nullable = false, updatable = false) private UUID sourceReferenceId;
	@CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	protected AssignmentSource() {}
	public AssignmentSource(UUID organizationId, UUID assignmentId, AssignmentOrigin origin, UUID sourceReferenceId) {
		this.organizationId = organizationId; this.assignmentId = assignmentId;
		this.origin = origin; this.sourceReferenceId = sourceReferenceId;
	}
	public AssignmentOrigin getOrigin() { return origin; }
	public UUID getSourceReferenceId() { return sourceReferenceId; }
}
