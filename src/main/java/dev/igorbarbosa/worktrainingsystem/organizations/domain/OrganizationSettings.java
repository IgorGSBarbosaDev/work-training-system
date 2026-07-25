package dev.igorbarbosa.worktrainingsystem.organizations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "organization_settings")
@EntityListeners(AuditingEntityListener.class)
public class OrganizationSettings {

	@Id
	@Column(name = "organization_id", nullable = false, updatable = false, insertable = false)
	private UUID organizationId;

	@Column(name = "expiring_soon_days", nullable = false)
	private int expiringSoonDays;

	@Column(name = "default_passing_score", nullable = false)
	private int defaultPassingScore;

	@Column(name = "default_required_video_percentage", nullable = false)
	private int defaultRequiredVideoPercentage;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected OrganizationSettings() {
	}

	public UUID getOrganizationId() { return organizationId; }
	public int getExpiringSoonDays() { return expiringSoonDays; }
	public int getDefaultPassingScore() { return defaultPassingScore; }
	public int getDefaultRequiredVideoPercentage() { return defaultRequiredVideoPercentage; }

	public void update(int expiringSoonDays, int defaultPassingScore) {
		this.expiringSoonDays = expiringSoonDays;
		this.defaultPassingScore = defaultPassingScore;
		this.defaultRequiredVideoPercentage = 80;
	}
}
