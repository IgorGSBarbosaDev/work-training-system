package dev.igorbarbosa.worktrainingsystem.activities.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ManualEmployeeActivityRequest(@NotNull UUID activityId, @Size(max = 1000) String reason) {}
