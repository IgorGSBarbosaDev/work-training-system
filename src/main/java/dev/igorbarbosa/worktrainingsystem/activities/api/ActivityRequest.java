package dev.igorbarbosa.worktrainingsystem.activities.api;

import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 2000) String description,
		@NotNull RegistrationStatus status) {}
