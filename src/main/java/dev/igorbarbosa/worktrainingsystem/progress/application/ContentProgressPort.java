package dev.igorbarbosa.worktrainingsystem.progress.application;

import java.util.UUID;

/** Read-only execution evidence exposed to assessment delivery. */
public interface ContentProgressPort {
	boolean requiredContentReady(UUID assignmentId, UUID trainingVersionId);
}
