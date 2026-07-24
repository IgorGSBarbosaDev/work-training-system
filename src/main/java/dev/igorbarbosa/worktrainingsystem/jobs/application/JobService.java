package dev.igorbarbosa.worktrainingsystem.jobs.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.jobs.api.CreateJobRequest;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

	private final JobRepository jobRepository;

	public JobService(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@Transactional
	public JobResponse create(CreateJobRequest request) {
		String name = request.name().trim();
		if (jobRepository.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)) {
			throw conflict();
		}

		Job job = new Job(
				DEFAULT_ORGANIZATION_ID,
				name,
				normalizeDescription(request.description()),
				request.status());
		try {
			return JobResponse.from(jobRepository.saveAndFlush(job));
		} catch (DataIntegrityViolationException exception) {
			throw conflict();
		}
	}

	@Transactional(readOnly = true)
	public Page<JobResponse> list(String search, RegistrationStatus status, Pageable pageable) {
		Specification<Job> specification = (root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
		String normalizedSearch = normalizeSearch(search);
		if (normalizedSearch != null) {
			specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedSearch + "%"),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + normalizedSearch + "%")));
		}
		if (status != null) {
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("status"), status));
		}
		return jobRepository.findAll(specification, pageable).map(JobResponse::from);
	}

	private String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}

	private String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
	}

	private ResourceConflictException conflict() {
		return new ResourceConflictException("JOB_ALREADY_EXISTS", "Já existe um cargo com o nome informado.");
	}
}
