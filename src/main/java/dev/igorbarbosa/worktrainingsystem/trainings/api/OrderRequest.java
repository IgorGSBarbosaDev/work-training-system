package dev.igorbarbosa.worktrainingsystem.trainings.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record OrderRequest(@NotEmpty List<@Valid Item> items) {
	public record Item(@NotNull UUID id, @Positive int order) {}
}
