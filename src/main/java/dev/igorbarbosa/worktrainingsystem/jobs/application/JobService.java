package dev.igorbarbosa.worktrainingsystem.jobs.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.jobs.api.CreateJobRequest;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.api.UpdateJobRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
public class JobService {

	private final JobRepository jobRepository;
	private final AuthorizationService authorization;

	public JobService(JobRepository jobRepository, AuthorizationService authorization) {
		this.jobRepository = jobRepository;
		this.authorization = authorization;
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
		Specification<Job> specification = visibility(authorization.currentScope());
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

	@Transactional(readOnly = true)
	public JobResponse get(UUID id) {
		return jobRepository.findOne(visibility(authorization.currentScope())
				.and((root, query, cb) -> cb.equal(root.get("id"), id)))
				.map(JobResponse::from).orElseThrow(() -> inaccessibleOrMissing(id));
	}

	@Transactional
	public JobResponse update(UUID id, UpdateJobRequest request) {
		if (!request.hasChanges()) throw noChanges();
		Job job = find(id);
		String name = request.name() == null ? job.getName() : request.name().trim();
		if (!name.equalsIgnoreCase(job.getName())
				&& jobRepository.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)) throw conflict();
		job.update(name, request.description() == null ? job.getDescription() : normalizeDescription(request.description()));
		try { jobRepository.flush(); } catch (DataIntegrityViolationException exception) { throw conflict(); }
		return JobResponse.from(job);
	}

	@Transactional
	public JobResponse changeStatus(UUID id, ChangeRegistrationStatusRequest request) {
		Job job = find(id);
		job.changeStatus(request.status());
		return JobResponse.from(job);
	}

	@Transactional(readOnly = true)
	public JobResponse getActive(UUID id) {
		Job job = find(id);
		if (job.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException("JOB_INACTIVE", "O cargo informado está inativo.");
		}
		return JobResponse.from(job);
	}

	private Job find(UUID id) {
		return jobRepository.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O cargo informado não existe."));
	}

	private Specification<Job> visibility(AccessScope scope) {
		Specification<Job> organization = (root, query, cb) -> cb.equal(root.get("organizationId"), scope.organizationId());
		if (scope.admin()) return organization;
		if (!scope.manager() || !scope.hasGrants()) return organization.and((root, query, cb) -> cb.disjunction());
		Set<UUID> jobIds = authorization.scopeReferences(scope).jobIds();
		return organization.and((root, query, cb) -> root.get("id").in(jobIds));
	}

	private RuntimeException inaccessibleOrMissing(UUID id) {
		return jobRepository.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID).isPresent()
				? new AccessDeniedException("O cargo está fora do escopo autorizado.")
				: new ResourceNotFoundException("O cargo informado não existe.");
	}

	private BusinessRuleViolationException noChanges() {
		return new BusinessRuleViolationException("NO_CHANGES", "Informe ao menos um campo para atualização.");
	}

	@Transactional(readOnly = true)
	public Map<UUID, JobResponse> getAllByIds(Set<UUID> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return jobRepository.findAllByIdInAndOrganizationId(ids, DEFAULT_ORGANIZATION_ID).stream()
				.map(JobResponse::from)
				.collect(Collectors.toMap(JobResponse::id, Function.identity()));
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
