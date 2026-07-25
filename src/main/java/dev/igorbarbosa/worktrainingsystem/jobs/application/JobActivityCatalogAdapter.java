package dev.igorbarbosa.worktrainingsystem.jobs.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JobActivityCatalogAdapter implements JobActivityCatalog {
	private final JobRepository jobs;
	JobActivityCatalogAdapter(JobRepository jobs) { this.jobs = jobs; }
	@Override @Transactional(readOnly = true) public JobSummary requireJob(UUID jobId) { return summary(find(jobId)); }
	@Override @Transactional(readOnly = true) public JobSummary requireActiveJob(UUID jobId) {
		Job job = find(jobId);
		if (job.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException("JOB_INACTIVE", "O cargo informado está inativo.");
		}
		return summary(job);
	}
	private Job find(UUID id) { return jobs.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
			.orElseThrow(() -> new ResourceNotFoundException("O cargo informado não existe.")); }
	private JobSummary summary(Job job) { return new JobSummary(job.getId(), job.getName(), job.getStatus() == RegistrationStatus.ACTIVE); }
}
