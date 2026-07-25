package dev.igorbarbosa.worktrainingsystem.employees.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.employees.api.CreateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeJobRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeJobResponse;
import dev.igorbarbosa.worktrainingsystem.employees.api.ChangeEmployeeStatusRequest;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeHistoryResponse;
import dev.igorbarbosa.worktrainingsystem.employees.api.UpdateEmployeeRequest;
import dev.igorbarbosa.worktrainingsystem.employees.domain.Employee;
import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistoryType;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeHistoryRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
import dev.igorbarbosa.worktrainingsystem.jobs.api.JobResponse;
import dev.igorbarbosa.worktrainingsystem.jobs.application.JobService;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.application.SectorService;
import dev.igorbarbosa.worktrainingsystem.organizations.application.UnitService;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final UnitService unitService;
	private final SectorService sectorService;
	private final JobService jobService;
	private final AuthorizationService authorization;
	private final EmployeeHistoryRecorder history;
	private final EmployeeHistoryRepository histories;
	private final ObjectStorage storage;
	private final ApplicationEventPublisher events;
	private final EmployeeLifecyclePort lifecycle;
	private final dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider currentUser;

	public EmployeeService(
			EmployeeRepository employeeRepository,
			UnitService unitService,
			SectorService sectorService,
			JobService jobService,
			AuthorizationService authorization,
			EmployeeHistoryRecorder history,
			EmployeeHistoryRepository histories,
			ObjectStorage storage,
			ApplicationEventPublisher events,
			EmployeeLifecyclePort lifecycle,
			dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider currentUser) {
		this.employeeRepository = employeeRepository;
		this.unitService = unitService;
		this.sectorService = sectorService;
		this.jobService = jobService;
		this.authorization = authorization;
		this.history = history;
		this.histories = histories;
		this.storage = storage;
		this.events = events;
		this.lifecycle = lifecycle;
		this.currentUser = currentUser;
	}

	@Transactional
	public EmployeeResponse create(CreateEmployeeRequest request) {
		String registration = request.registration().trim();
		if (employeeRepository.existsByOrganizationIdAndRegistrationIgnoreCase(
				DEFAULT_ORGANIZATION_ID, registration)) {
			throw registrationConflict();
		}
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (employeeRepository.existsByOrganizationIdAndEmailIgnoreCase(DEFAULT_ORGANIZATION_ID, email)) {
			throw emailConflict();
		}

		UnitResponse unit = unitService.getActive(request.unitId());
		SectorResponse sector = sectorService.getActive(request.sectorId());
		if (!sector.unitId().equals(unit.id())) {
			throw new BusinessRuleViolationException(
					"SECTOR_UNIT_MISMATCH", "O setor informado não pertence à unidade selecionada.");
		}
		JobResponse job = jobService.getActive(request.jobId());

		Employee employee = new Employee(
				DEFAULT_ORGANIZATION_ID,
				request.name().trim(),
				registration,
				email,
				job.id(),
				sector.id(),
				unit.id(),
				request.status());
		try {
			Employee saved = employeeRepository.saveAndFlush(employee);
			history.record(saved, EmployeeHistoryType.CREATED, null);
			lifecycle.initialize(lifecycleData(saved), currentUser.requireCurrentUser().userId());
			return EmployeeResponse.from(saved, job, sector, unit, null);
		} catch (DataIntegrityViolationException exception) {
			throw translateIntegrityViolation(exception);
		}
	}

	@Transactional(readOnly = true)
	public EmployeeResponse getById(UUID employeeId) {
		Employee employee = findAccessibleEmployee(employeeId);
		return toResponse(employee, referencesFor(Set.of(employee)));
	}

	@Transactional(readOnly = true)
	public EmployeeResponse getByRegistration(String registration) {
		AccessScope scope = authorization.currentScope();
		Specification<Employee> specification = baseSpecification(scope)
				.and((root, query, cb) -> cb.equal(cb.lower(root.get("registration")),
						registration.trim().toLowerCase(Locale.ROOT)));
		Employee employee = employeeRepository.findOne(specification)
				.orElseThrow(() -> inaccessibleOrMissingRegistration(registration));
		return toResponse(employee, referencesFor(Set.of(employee)));
	}

	@Transactional(readOnly = true)
	public Page<EmployeeResponse> list(
			String search,
			String registration,
			String email,
			UUID unitId,
			UUID sectorId,
			UUID jobId,
			RegistrationStatus status,
			Pageable pageable) {
		Specification<Employee> specification = baseSpecification(authorization.currentScope());
		String normalizedSearch = normalize(search);
		if (normalizedSearch != null) {
			specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedSearch + "%"),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("registration")), "%" + normalizedSearch + "%"),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + normalizedSearch + "%")));
		}
		specification = addTextFilter(specification, "registration", registration);
		specification = addTextFilter(specification, "email", email);
		if (unitId != null) {
			specification = specification.and(equalsFilter("unitId", unitId));
		}
		if (sectorId != null) {
			specification = specification.and(equalsFilter("sectorId", sectorId));
		}
		if (jobId != null) {
			specification = specification.and(equalsFilter("jobId", jobId));
		}
		if (status != null) {
			specification = specification.and(equalsFilter("status", status));
		}

		Page<Employee> employees = employeeRepository.findAll(specification, pageable);
		ReferenceData references = referencesFor(Set.copyOf(employees.getContent()));
		return new PageImpl<>(
				employees.getContent().stream().map(employee -> toResponse(employee, references)).toList(),
				pageable,
				employees.getTotalElements());
	}

	@Transactional
	public EmployeeResponse update(UUID employeeId, UpdateEmployeeRequest request) {
		if (!request.hasChanges()) {
			throw new BusinessRuleViolationException(
					"NO_CHANGES", "Informe ao menos um campo para atualização.");
		}

		Employee employee = findEmployee(employeeId);
		Map<String, Object> before = history.snapshot(employee);
		String registration = request.registration() == null
				? employee.getRegistration()
				: request.registration().trim();
		if (!registration.equalsIgnoreCase(employee.getRegistration())
				&& employeeRepository.existsByOrganizationIdAndRegistrationIgnoreCase(
						DEFAULT_ORGANIZATION_ID, registration)) {
			throw registrationConflict();
		}
		String email = request.email() == null ? employee.getEmail()
				: request.email().trim().toLowerCase(Locale.ROOT);
		if (!email.equalsIgnoreCase(employee.getEmail())
				&& employeeRepository.existsByOrganizationIdAndEmailIgnoreCase(DEFAULT_ORGANIZATION_ID, email)) {
			throw emailConflict();
		}

		UUID unitId = request.unitId() == null ? employee.getUnitId() : request.unitId();
		UUID sectorId = request.sectorId() == null ? employee.getSectorId() : request.sectorId();
		if (request.unitId() != null || request.sectorId() != null) {
			UnitResponse unit = unitService.getActive(unitId);
			SectorResponse sector = sectorService.getActive(sectorId);
			if (!sector.unitId().equals(unit.id())) {
				throw new BusinessRuleViolationException(
						"SECTOR_UNIT_MISMATCH", "O setor informado não pertence à unidade selecionada.");
			}
		}

		employee.updateProfile(
				request.name() == null ? employee.getName() : request.name().trim(),
				registration,
				email,
				sectorId,
				unitId);
		try {
			employeeRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw translateIntegrityViolation(exception);
		}
		history.record(employee, EmployeeHistoryType.PROFILE_UPDATED, before);
		return toResponse(employee, referencesFor(Set.of(employee)));
	}

	@Transactional
	public EmployeeResponse changeStatus(UUID employeeId, ChangeEmployeeStatusRequest request) {
		Employee employee = findEmployee(employeeId);
		Map<String, Object> before = history.snapshot(employee);
		if (request.status() == RegistrationStatus.ACTIVE
				&& employee.getStatus() != RegistrationStatus.ACTIVE) {
			UnitResponse unit = unitService.getActive(employee.getUnitId());
			SectorResponse sector = sectorService.getActive(employee.getSectorId());
			if (!sector.unitId().equals(unit.id())) {
				throw new BusinessRuleViolationException(
						"SECTOR_UNIT_MISMATCH", "O setor informado não pertence à unidade selecionada.");
			}
			jobService.getActive(employee.getJobId());
		}
		employee.changeStatus(request.status());
		employeeRepository.flush();
		history.record(employee, EmployeeHistoryType.STATUS_CHANGED, before);
		return toResponse(employee, referencesFor(Set.of(employee)));
	}

	@Transactional
	public ChangeEmployeeJobResponse changeJob(UUID employeeId, ChangeEmployeeJobRequest request) {
		Employee employee = findEmployee(employeeId);
		if (employee.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException(
					"EMPLOYEE_INACTIVE", "Colaborador inativo não pode receber um novo cargo.");
		}
		JobResponse job = jobService.getActive(request.jobId());
		Map<String, Object> before = history.snapshot(employee);
		UUID previousJobId = employee.getJobId();
		employee.changeJob(job.id());
		history.record(employee, EmployeeHistoryType.JOB_CHANGED, before);
		var effects = lifecycle.changeJob(lifecycleData(employee), previousJobId,
				request.removePreviousJobActivities(), currentUser.requireCurrentUser().userId());
		return new ChangeEmployeeJobResponse(employee.getId(), previousJobId, job.id(), effects.activitiesAdded(),
				effects.activitiesRemoved(), effects.assignmentsCreated(), effects.qualificationsRecalculated());
	}

	@Transactional(readOnly = true)
	public Page<EmployeeHistoryResponse> history(UUID employeeId, Pageable pageable) {
		findAccessibleEmployee(employeeId);
		return histories.findAllByEmployeeId(employeeId, pageable)
				.map(item -> EmployeeHistoryResponse.from(item, item.getBeforeState(), item.getAfterState()));
	}

	@Transactional
	public EmployeeResponse replacePhoto(UUID employeeId, String objectKey, String contentType, long sizeBytes) {
		Employee employee = findAccessibleEmployee(employeeId);
		Map<String, Object> before = history.snapshot(employee);
		String oldObjectKey = employee.getPhotoObjectKey();
		employee.replacePhoto(objectKey, contentType, sizeBytes);
		history.record(employee, EmployeeHistoryType.PHOTO_UPDATED, before);
		if (oldObjectKey != null) events.publishEvent(new EmployeePhotoReplaced(oldObjectKey));
		return toResponse(employee, referencesFor(Set.of(employee)));
	}

	@Transactional
	public void removePhoto(UUID employeeId) {
		Employee employee = findAccessibleEmployee(employeeId);
		if (employee.getPhotoObjectKey() == null) return;
		Map<String, Object> before = history.snapshot(employee);
		String oldObjectKey = employee.getPhotoObjectKey();
		employee.removePhoto();
		history.record(employee, EmployeeHistoryType.PHOTO_REMOVED, before);
		events.publishEvent(new EmployeePhotoReplaced(oldObjectKey));
	}

	@Transactional(readOnly = true)
	public Page<EmployeeResponse> listByUnit(UUID unitId, Pageable pageable) {
		return listForReference("unitId", unitId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<EmployeeResponse> listBySector(UUID sectorId, Pageable pageable) {
		return listForReference("sectorId", sectorId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<EmployeeResponse> listByJob(UUID jobId, Pageable pageable) {
		return listForReference("jobId", jobId, pageable);
	}

	private Page<EmployeeResponse> listForReference(String property, UUID value, Pageable pageable) {
		Page<Employee> employees = employeeRepository.findAll(baseSpecification(authorization.currentScope())
				.and(equalsFilter(property, value)), pageable);
		ReferenceData references = referencesFor(Set.copyOf(employees.getContent()));
		return employees.map(employee -> toResponse(employee, references));
	}

	private Specification<Employee> addTextFilter(
			Specification<Employee> specification, String property, String value) {
		String normalized = normalize(value);
		return normalized == null
				? specification
				: specification.and((root, query, criteriaBuilder) ->
						criteriaBuilder.equal(criteriaBuilder.lower(root.get(property)), normalized));
	}

	private Specification<Employee> equalsFilter(String property, Object value) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(property), value);
	}

	private ReferenceData referencesFor(Set<Employee> employees) {
		Set<UUID> unitIds = employees.stream().map(Employee::getUnitId).collect(Collectors.toSet());
		Set<UUID> sectorIds = employees.stream().map(Employee::getSectorId).collect(Collectors.toSet());
		Set<UUID> jobIds = employees.stream().map(Employee::getJobId).collect(Collectors.toSet());
		return new ReferenceData(
				unitService.getAllByIds(unitIds),
				sectorService.getAllByIds(sectorIds),
				jobService.getAllByIds(jobIds));
	}

	private Employee findEmployee(UUID employeeId) {
		return employeeRepository.findByIdAndOrganizationId(employeeId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O colaborador informado não existe."));
	}

	private Employee findAccessibleEmployee(UUID employeeId) {
		AccessScope scope = authorization.currentScope();
		return employeeRepository.findOne(baseSpecification(scope).and(equalsFilter("id", employeeId)))
				.orElseThrow(() -> inaccessibleOrMissing(employeeId));
	}

	private RuntimeException inaccessibleOrMissing(UUID employeeId) {
		if (employeeRepository.findByIdAndOrganizationId(employeeId, DEFAULT_ORGANIZATION_ID).isPresent()) {
			return new AccessDeniedException("O colaborador está fora do escopo autorizado.");
		}
		return new ResourceNotFoundException("O colaborador informado não existe.");
	}

	private RuntimeException inaccessibleOrMissingRegistration(String registration) {
		if (employeeRepository.findByOrganizationIdAndRegistrationIgnoreCase(DEFAULT_ORGANIZATION_ID,
				registration.trim()).isPresent()) {
			return new AccessDeniedException("O colaborador está fora do escopo autorizado.");
		}
		return new ResourceNotFoundException("O colaborador informado não existe.");
	}

	private Specification<Employee> baseSpecification(AccessScope scope) {
		Specification<Employee> organization = (root, query, cb) ->
				cb.equal(root.get("organizationId"), scope.organizationId());
		if (scope.admin()) return organization;
		if (scope.employee()) {
			return organization.and((root, query, cb) -> scope.ownEmployeeId() == null
					? cb.disjunction() : cb.equal(root.get("id"), scope.ownEmployeeId()));
		}
		if (!scope.manager() || !scope.hasGrants()) {
			return organization.and((root, query, cb) -> cb.disjunction());
		}
		return organization.and((root, query, cb) -> cb.and(
				cb.equal(root.get("status"), RegistrationStatus.ACTIVE),
				cb.or(root.get("unitId").in(scope.unitIds()), root.get("sectorId").in(scope.sectorIds()),
						root.get("id").in(scope.employeeIds()))));
	}

	private EmployeeResponse toResponse(Employee employee, ReferenceData references) {
		return EmployeeResponse.from(
				employee,
				references.jobs().get(employee.getJobId()),
				references.sectors().get(employee.getSectorId()),
				references.units().get(employee.getUnitId()),
				employee.getPhotoObjectKey() == null ? null
						: storage.presignDownload(employee.getPhotoObjectKey()).url().toString());
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	private EmployeeLifecyclePort.EmployeeData lifecycleData(Employee employee) {
		return new EmployeeLifecyclePort.EmployeeData(employee.getId(), employee.getOrganizationId(), employee.getJobId(),
				employee.getStatus() == RegistrationStatus.ACTIVE);
	}

	private ResourceConflictException registrationConflict() {
		return new ResourceConflictException(
				"REGISTRATION_ALREADY_EXISTS", "Já existe um colaborador com a matrícula informada.");
	}

	private ResourceConflictException emailConflict() {
		return new ResourceConflictException("EMAIL_ALREADY_EXISTS",
				"Já existe um colaborador com o e-mail informado.");
	}

	private RuntimeException translateIntegrityViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause.getMessage() != null
					&& cause.getMessage().contains("uk_employees_organization_registration")) {
				return registrationConflict();
			}
			if (cause.getMessage() != null
					&& cause.getMessage().contains("uk_employees_organization_email_lower")) {
				return emailConflict();
			}
			cause = cause.getCause();
		}
		return exception;
	}

	private record ReferenceData(
			Map<UUID, UnitResponse> units,
			Map<UUID, SectorResponse> sectors,
			Map<UUID, JobResponse> jobs) {
	}
}
