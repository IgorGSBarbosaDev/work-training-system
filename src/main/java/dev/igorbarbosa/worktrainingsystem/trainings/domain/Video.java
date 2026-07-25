package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

@Entity
@Table(name = "training_videos")
public class Video extends BaseEntity {
	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(name = "module_id", nullable = false, updatable = false)
	private UUID moduleId;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "duration_seconds", nullable = false)
	private int durationSeconds;

	@Column(name = "storage_object_key", nullable = false, length = 2048)
	private String storageObjectKey;

	@Column(name = "file_id")
	private UUID fileId;

	@Column(nullable = false)
	private boolean required;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Video() {
	}

	public Video(UUID moduleId, String title, String description, int displayOrder, int durationSeconds,
			String storageObjectKey, boolean required, RegistrationStatus status) {
		this(moduleId, title, description, displayOrder, durationSeconds, null, storageObjectKey, required, status);
	}

	public Video(UUID moduleId, String title, String description, int displayOrder, int durationSeconds,
			UUID fileId, String storageObjectKey, boolean required, RegistrationStatus status) {
		this.organizationId = DEFAULT_ORGANIZATION_ID;
		this.moduleId = moduleId;
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
		this.durationSeconds = durationSeconds;
		this.storageObjectKey = storageObjectKey;
		this.fileId = fileId;
		this.required = required;
		this.status = status;
	}

	public UUID getModuleId() {
		return moduleId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public int getDurationSeconds() {
		return durationSeconds;
	}

	public String getStorageObjectKey() {
		return storageObjectKey;
	}

	public UUID getFileId() { return fileId; }
	public UUID getOrganizationId() { return organizationId; }

	public boolean isRequired() {
		return required;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String title, String description, int displayOrder, int durationSeconds,
			UUID fileId, String storageObjectKey, boolean required) {
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
		this.durationSeconds = durationSeconds;
		this.storageObjectKey = storageObjectKey;
		this.fileId = fileId;
		this.required = required;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
	public void changeOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
