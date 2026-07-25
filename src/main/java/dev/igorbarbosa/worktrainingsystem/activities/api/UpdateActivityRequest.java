package dev.igorbarbosa.worktrainingsystem.activities.api;

import jakarta.validation.constraints.Size;

public record UpdateActivityRequest(@Size(min = 1, max = 150) String name, @Size(max = 2000) String description) {
	public boolean hasChanges() { return name != null || description != null; }
}
