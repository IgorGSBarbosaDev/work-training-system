package dev.igorbarbosa.worktrainingsystem.progress.api;

import java.util.UUID;

public record ResumePointResponse(UUID assignmentId, UUID moduleId, UUID videoId, long positionSeconds) {
}
