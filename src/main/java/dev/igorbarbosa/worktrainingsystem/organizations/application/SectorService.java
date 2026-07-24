package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateSectorRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.SectorResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
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

@Service
public class SectorService {

	private final SectorRepository sectorRepository;
	private final UnitRepository unitRepository;

	public SectorService(SectorRepository sectorRepository, UnitRepository unitRepository) {
		this.sectorRepository = sectorRepository;
		this.unitRepository = unitRepository;
	}

	@Transactional
	public SectorResponse create(CreateSectorRequest request) {
		Unit unit = unitRepository.findByIdAndOrganizationId(request.unitId(), DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A unidade informada não existe."));
		String name = request.name().trim();
		String code = normalizeCode(request.code());
		if (sectorRepository.existsByUnitIdAndNameIgnoreCase(unit.getId(), name)
				|| code != null && sectorRepository.existsByUnitIdAndCodeIgnoreCase(unit.getId(), code)) {
			throw conflict();
		}

		Sector sector = new Sector(DEFAULT_ORGANIZATION_ID, unit, name, code, request.status());
		try {
			return SectorResponse.from(sectorRepository.saveAndFlush(sector));
		} catch (DataIntegrityViolationException exception) {
			throw conflict();
		}
	}

	@Transactional(readOnly = true)
	public Page<SectorResponse> list(
			String search, RegistrationStatus status, UUID unitId, Pageable pageable) {
		Specification<Sector> specification = (root, query, criteriaBuilder) ->
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
		if (unitId != null) {
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("unit").get("id"), unitId));
		}
		return sectorRepository.findAll(specification, pageable).map(SectorResponse::from);
	}

	@Transactional(readOnly = true)
	public SectorResponse getActive(UUID id) {
		Sector sector = sectorRepository.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("O setor informado não existe."));
		if (sector.getStatus() != RegistrationStatus.ACTIVE) {
			throw new BusinessRuleViolationException("SECTOR_INACTIVE", "O setor informado está inativo.");
		}
		return SectorResponse.from(sector);
	}

	@Transactional(readOnly = true)
	public Map<UUID, SectorResponse> getAllByIds(Set<UUID> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return sectorRepository.findAllByIdInAndOrganizationId(ids, DEFAULT_ORGANIZATION_ID).stream()
				.map(SectorResponse::from)
				.collect(Collectors.toMap(SectorResponse::id, Function.identity()));
	}

	private String normalizeCode(String code) {
		return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
	}

	private ResourceConflictException conflict() {
		return new ResourceConflictException(
				"SECTOR_ALREADY_EXISTS", "Já existe um setor com o nome ou código informado nessa unidade.");
	}
}
