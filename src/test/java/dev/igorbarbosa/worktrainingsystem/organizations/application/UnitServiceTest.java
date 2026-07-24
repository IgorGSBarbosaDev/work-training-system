package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.organizations.api.CreateUnitRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Unit;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.UnitRepository;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

	@Mock
	private UnitRepository unitRepository;

	@InjectMocks
	private UnitService unitService;

	@Test
	void createsUnitWithNormalizedValues() {
		when(unitRepository.saveAndFlush(any(Unit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		unitService.create(new CreateUnitRequest("  Unidade Norte  ", " nor-1 ", RegistrationStatus.ACTIVE));

		ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
		verify(unitRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
		assertThat(captor.getValue().getName()).isEqualTo("Unidade Norte");
		assertThat(captor.getValue().getCode()).isEqualTo("NOR-1");
	}

	@Test
	void rejectsDuplicatedUnitNameIgnoringCase() {
		when(unitRepository.existsByOrganizationIdAndNameIgnoreCase(DEFAULT_ORGANIZATION_ID, "Unidade Norte"))
				.thenReturn(true);

		assertThatThrownBy(() -> unitService.create(
				new CreateUnitRequest("Unidade Norte", "NOR", RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class)
				.hasMessage("Já existe uma unidade com o nome ou código informado.");
	}

	@Test
	void translatesDatabaseUniquenessConflict() {
		when(unitRepository.saveAndFlush(any(Unit.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> unitService.create(
				new CreateUnitRequest("Unidade Norte", "NOR", RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("UNIT_ALREADY_EXISTS");
	}
}
