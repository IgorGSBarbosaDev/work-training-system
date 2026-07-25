package dev.igorbarbosa.worktrainingsystem.activities.api;

import java.time.Instant;
import java.util.UUID;

public record RelatedJobResponse(UUID id, String name, Instant linkedAt) {}
