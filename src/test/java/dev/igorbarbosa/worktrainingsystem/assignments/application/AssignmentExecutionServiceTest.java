package dev.igorbarbosa.worktrainingsystem.assignments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentOrigin;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentPriority;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentStatusEventRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssignmentExecutionServiceTest {
	@Mock TrainingAssignmentRepository assignments;
	@Mock AssignmentStatusEventRepository events;
	@Mock AuthorizationService authorization;
	@Mock CurrentUserProvider currentUser;
	@Mock TrainingReadinessPort readiness;
	private final Instant now = Instant.parse("2026-07-24T12:00:00Z");

	@Test
	void onlyExactOwnerStartsAndQuestionnaireReadinessAdvancesStatus() {
		UUID employeeId = UUID.randomUUID(); TrainingAssignment item = assignment(employeeId);
		when(assignments.findByIdAndOrganizationId(item.getId(), DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(item));
		when(currentUser.requireCurrentUser()).thenReturn(user(employeeId));
		AssignmentExecutionService service = service();
		assertThat(service.start(item.getId()).status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
		assertThat(service.contentReady(item.getId(), true).status()).isEqualTo(AssignmentStatus.AWAITING_ASSESSMENT);
		verify(events, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void rejectsDifferentEmployeeAndUsesConservativeReadinessPortWithoutCompleting() {
		TrainingAssignment item = assignment(UUID.randomUUID());
		when(assignments.findByIdAndOrganizationId(item.getId(), DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(item));
		when(currentUser.requireCurrentUser()).thenReturn(user(UUID.randomUUID()));
		assertThatThrownBy(() -> service().start(item.getId())).isInstanceOf(AccessDeniedException.class);

		UUID owner = item.getEmployeeId(); when(currentUser.requireCurrentUser()).thenReturn(user(owner));
		AssignmentExecutionService service = service(); service.start(item.getId());
		assertThat(service.contentReady(item.getId(), false).status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
		verify(readiness).contentReady(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void noQuestionnaireReadinessCreatesCompletionAndClosesAssignment() {
		UUID employeeId = UUID.randomUUID(); TrainingAssignment item = assignment(employeeId);
		when(assignments.findByIdAndOrganizationId(item.getId(), DEFAULT_ORGANIZATION_ID)).thenReturn(Optional.of(item));
		when(currentUser.requireCurrentUser()).thenReturn(user(employeeId));
		when(readiness.contentReady(org.mockito.ArgumentMatchers.any())).thenReturn(true);
		AssignmentExecutionService service = service(); service.start(item.getId());
		assertThat(service.contentReady(item.getId(), false).status()).isEqualTo(AssignmentStatus.COMPLETED);
		verify(events, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
	}

	private AssignmentExecutionService service() {
		return new AssignmentExecutionService(assignments, events, authorization, currentUser, readiness,
				Clock.fixed(now, ZoneOffset.UTC));
	}
	private TrainingAssignment assignment(UUID employeeId) {
		TrainingAssignment item = new TrainingAssignment(DEFAULT_ORGANIZATION_ID, employeeId, UUID.randomUUID(), UUID.randomUUID(),
				AssignmentOrigin.EMPLOYEE, now.minusSeconds(60), null, AssignmentPriority.NORMAL, UUID.randomUUID(),
				null, null, null, null);
		ReflectionTestUtils.setField(item, "id", UUID.randomUUID()); return item;
	}
	private CurrentUser user(UUID employeeId) {
		return new CurrentUser(UUID.randomUUID(), DEFAULT_ORGANIZATION_ID, UserRole.EMPLOYEE, employeeId, Set.of());
	}
}
