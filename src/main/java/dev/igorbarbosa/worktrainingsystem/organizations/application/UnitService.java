package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.ChangeRegistrationStatusRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService.AccessScope;
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
public class UnitService {

	private final UnitRepository unitRepository;
	private final SectorRepository sectorRepository;
	private final AuthorizationService authorization;

	public UnitService(UnitRepository unitRepository, SectorRepository sectorRepository,
			AuthorizationService authorization) {
		this.unitRepository = unitRepository;
		this.sectorRepository = sectorRepository;
		this.authorization = authorization;
	}

	@Transactional
	public UnitResponse create(CreateUnitRequest request) {
		String name = request.name().trim();
		String code = normalizeCode(request.code());
		if (unitRepository.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)
				|| code != null && unitRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, code)) {
			throw conflict();
		}

		Unit unit = new Unit(DEFAULT_ORGANIZATION_ID, name, code, request.status());
		try {
			return UnitResponse.from(unitRepository.saveAndFlush(unit));
		} catch (DataIntegrityViolationException exception) {
			throw conflict();
		}
	}

	@Transactional(readOnly = true)
	public Page<UnitResponse> list(String search, RegistrationStatus status, Pageable pageable) {
		Specification<Unit> specification = visibility(authorization.currentScope());
		String normalizedSearch = normalizeSearch(search);
		if (normalizedSearch != null) {
			specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedSearch + "%"),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), "%" + normalizedSearch + "%")));
		}
		if (status != null) {
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("status"), status));
		}
		return unitRepository.findAll(specification, pageable).map(UnitResponse::from);
	}

	@Transactional(readOnly = true)
	public UnitResponse get(UUID id) {
		AccessScope scope = authorization.currentScope();
		return unitRepository.findOne(visibility(scope).and((root, query, cb) -> cb.equal(root.get("id"), id)))
				.map(UnitResponse::from).orElseThrow(() -> inaccessibleOrMissing(id));
	}

	@Transactional
	public UnitResponse update(UUID id, UpdateUnitRequest request) {
		if (!request.hasChanges()) throw noChanges();
		Unit unit = find(id);
		String name = request.name() == null ? unit.getName() : request.name().trim();
		String code = request.code() == null ? unit.getCode() : normalizeCode(request.code());
		if (!name.equalsIgnoreCase(unit.getName())
				&& unitRepository.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, name)
				|| code != null && !code.equalsIgnoreCase(String.valueOf(unit.getCode()))
				&& unitRepository.existsByOrganizationIdAndCodeIgnoreCase(DEFAULT_ORGANIZATION_ID, code)) throw conflict();
		unit.update(name, code);
		try { unitRepository.flush(); } catch (DataIntegrityViolationException exception) { throw conflict(); }
		return UnitResponse.from(unit);
	}

	@Transactional
	public UnitResponse changeStatus(UUID id, ChangeRegistrationStatusRequest request) {
		Unit unit = find(id);
		unit.changeStatus(request.status());
		return UnitResponse.from(unit);
	}

	@Transactional(readOnly = true)
	public UnitResponse getActive(UUID id) {
		Unit unit = find(id);
		if (unit.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException("UNIT_INACTIVE", "A unidade informada está inativa.");
		}
		return UnitResponse.from(unit);
	}

	private Unit find(UUID id) {
		return unitRepository.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A unidade informada não existe."));
	}

	private Specification<Unit> visibility(AccessScope scope) {
		Specification<Unit> organization = (root, query, cb) -> cb.equal(root.get("organizationId"), scope.organizationId());
		if (scope.admin()) return organization;
		if (!scope.manager() || !scope.hasGrants()) return organization.and((root, query, cb) -> cb.disjunction());
		Set<UUID> visible = new java.util.HashSet<>(scope.unitIds());
		visible.addAll(authorization.scopeReferences(scope).unitIds());
		visible.addAll(sectorRepository.findAllByIdInAndOrganizationId(scope.sectorIds(), scope.organizationId()).stream()
				.map(sector -> sector.getUnit().getId()).toList());
		return organization.and((root, query, cb) -> root.get("id").in(visible));
	}

	private RuntimeException inaccessibleOrMissing(UUID id) {
		return unitRepository.existsByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				? new AccessDeniedException("A unidade está fora do escopo autorizado.")
				: new ResourceNotFoundException("A unidade informada não existe.");
	}

	private BusinessRuleViolationException noChanges() {
		return new BusinessRuleViolationException("NO_CHANGES", "Informe ao menos um campo para atualização.");
	}

	@Transactional(readOnly = true)
	public Map<UUID, UnitResponse> getAllByIds(Set<UUID> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return unitRepository.findAllByIdInAndOrganizationId(ids, DEFAULT_ORGANIZATION_ID).stream()
				.map(UnitResponse::from)
				.collect(Collectors.toMap(UnitResponse::id, Function.identity()));
	}

	private String normalizeCode(String code) {
		return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
	}

	private ResourceConflictException conflict() {
		return new ResourceConflictException(
				"UNIT_ALREADY_EXISTS", "Já existe uma unidade com o nome ou código informado.");
	}
}
