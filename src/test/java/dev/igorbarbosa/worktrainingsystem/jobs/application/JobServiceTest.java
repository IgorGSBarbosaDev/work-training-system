package dev.igorbarbosa.worktrainingsystem.jobs.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.jobs.api.CreateJobRequest;
import dev.igorbarbosa.worktrainingsystem.jobs.domain.Job;
import dev.igorbarbosa.worktrainingsystem.jobs.persistence.JobRepository;
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
class JobServiceTest {

	@Mock
	private JobRepository jobRepository;

	@InjectMocks
	private JobService jobService;

	@Test
	void createsJobWithNormalizedValues() {
		when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

		jobService.create(new CreateJobRequest(
				"  Operador Industrial  ", "  Opera máquinas.  ", RegistrationStatus.ACTIVE));

		ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
		verify(jobRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Operador Industrial");
		assertThat(captor.getValue().getDescription()).isEqualTo("Opera máquinas.");
	}

	@Test
	void rejectsDuplicatedJobNameIgnoringCase() {
		when(jobRepository.existsByOrganizationIdAndNameIgnoreCase(
				DEFAULT_ORGANIZATION_ID, "Operador Industrial")).thenReturn(true);

		assertThatThrownBy(() -> jobService.create(new CreateJobRequest(
				"Operador Industrial", null, RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class)
				.hasMessage("Já existe um cargo com o nome informado.");
	}

	@Test
	void translatesDatabaseUniquenessConflict() {
		when(jobRepository.saveAndFlush(any(Job.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> jobService.create(new CreateJobRequest(
				"Operador Industrial", null, RegistrationStatus.ACTIVE)))
				.isInstanceOf(ResourceConflictException.class)
				.extracting("code")
				.isEqualTo("JOB_ALREADY_EXISTS");
	}
}
