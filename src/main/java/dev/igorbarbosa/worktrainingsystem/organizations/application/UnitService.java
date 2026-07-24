package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UnitResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
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
public class UnitService {

	private final UnitRepository unitRepository;

	public UnitService(UnitRepository unitRepository) {
		this.unitRepository = unitRepository;
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
		Specification<Unit> specification = (root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("organizationId"), DEFAULT_ORGANIZATION_ID);
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
