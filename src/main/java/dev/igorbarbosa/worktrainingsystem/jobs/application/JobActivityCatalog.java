package dev.igorbarbosa.worktrainingsystem.jobs.application;

import java.util.UUID;

/** Public job boundary used by the activities module. */
public interface JobActivityCatalog {
	JobSummary requireJob(UUID jobId);
	JobSummary requireActiveJob(UUID jobId);
	record JobSummary(UUID id, String name, boolean active) {}
}
