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
@Table(name = "training_modules")
public class TrainingModule extends BaseEntity {

	@Column(name = "training_version_id", nullable = false, updatable = false)
	private UUID trainingVersionId;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RegistrationStatus status;

	protected TrainingModule() {
	}

	public TrainingModule(UUID trainingVersionId, String title, String description, int displayOrder,
			RegistrationStatus status) {
		this.trainingVersionId = trainingVersionId;
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
		this.status = status;
	}

	public UUID getTrainingVersionId() {
		return trainingVersionId;
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

	public RegistrationStatus getStatus() {
		return status;
	}

	public void update(String title, String description, int displayOrder) {
		this.title = title;
		this.description = description;
		this.displayOrder = displayOrder;
	}

	public void changeStatus(RegistrationStatus status) {
		this.status = status;
	}
	public void changeOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
