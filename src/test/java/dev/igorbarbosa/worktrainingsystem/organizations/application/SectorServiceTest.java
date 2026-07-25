package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateSectorRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Sector;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.SectorRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

	@Mock
	private SectorRepository sectorRepository;

	@Mock
	private UnitRepository unitRepository;

	@InjectMocks
	private SectorService sectorService;

	@Test
	void rejectsSectorWhenUnitDoesNotExist() {
		UUID unitId = UUID.randomUUID();
		when(unitRepository.findByIdAndOrganizationId(unitId, DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> sectorService.create(
				new CreateSectorRequest(unitId, "Manutenção", "MAN", RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("A unidade informada não existe.");
	}

	@Test
	void createsSectorWithinTheSelectedUnit() {
		UUID unitId = UUID.randomUUID();
		Unit unit = mock(Unit.class);
		when(unit.getId()).thenReturn(unitId);
		when(unit.getStatus()).thenReturn(RegistrationStatus.ACTIVE);
		when(unitRepository.findByIdAndOrganizationId(unitId, DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(unit));
		when(sectorRepository.saveAndFlush(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

		sectorService.create(new CreateSectorRequest(
				unitId, "  Manutenção  ", " man ", RegistrationStatus.ACTIVE));

		ArgumentCaptor<Sector> captor = ArgumentCaptor.forClass(Sector.class);
		verify(sectorRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUnit()).isSameAs(unit);
		assertThat(captor.getValue().getName()).isEqualTo("Manutenção");
		assertThat(captor.getValue().getCode()).isEqualTo("MAN");
	}

	@Test
	void rejectsDuplicatedSectorWithinUnit() {
		UUID unitId = UUID.randomUUID();
		Unit unit = mock(Unit.class);
		when(unit.getId()).thenReturn(unitId);
		when(unit.getStatus()).thenReturn(RegistrationStatus.ACTIVE);
		when(unitRepository.findByIdAndOrganizationId(unitId, DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(unit));
		when(sectorRepository.existsByUnitIdAndNameIgnoreCase(unitId, "Manutenção")).thenReturn(true);

		assertThatThrownBy(() -> sectorService.create(
				new CreateSectorRequest(unitId, "Manutenção", "MAN", RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class);
	}

	@Test
	void translatesDatabaseUniquenessConflict() {
		UUID unitId = UUID.randomUUID();
		Unit unit = mock(Unit.class);
		when(unit.getId()).thenReturn(unitId);
		when(unit.getStatus()).thenReturn(RegistrationStatus.ACTIVE);
		when(unitRepository.findByIdAndOrganizationId(unitId, DEFAULT_ORGANIZATION_ID))
				.thenReturn(Optional.of(unit));
		when(sectorRepository.saveAndFlush(any(Sector.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> sectorService.create(
				new CreateSectorRequest(unitId, "Manutenção", "MAN", RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("SECTOR_ALREADY_EXISTS");
	}
}
