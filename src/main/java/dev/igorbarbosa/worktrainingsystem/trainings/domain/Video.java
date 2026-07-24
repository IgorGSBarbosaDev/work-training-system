package dev.igorbarbosa.worktrainingsystem.trainings.domain;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "training_videos")
public class Video extends BaseEntity {

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

	@Column(nullable = false)
	private boolean required;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected Video() {
	}

	public Video(UUID moduleId, String title, String description, int displayOrder, int durationSeconds,
			String storageObjectKey, boolean required, RegistrationStatus status) {
		this.moduleId = moduleId;
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
		this.durationSeconds = durationSeconds;
		this.storageObjectKey = storageObjectKey;
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

	public boolean isRequired() {
		return required;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String title, String description, int displayOrder, int durationSeconds,
			String storageObjectKey, boolean required) {
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
		this.durationSeconds = durationSeconds;
		this.storageObjectKey = storageObjectKey;
		this.required = required;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
}
