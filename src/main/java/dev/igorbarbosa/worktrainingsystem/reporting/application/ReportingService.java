package dev.igorbarbosa.worktrainingsystem.reporting.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardOverviewResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.PersonalDashboardResponse;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.persistence.TrainingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ReportingService {
	private final EmployeeRepository employees; private final TrainingRepository trainings; private final TrainingAssignmentRepository assignments; private final TrainingCompletionRepository completions; private final CurrentUserProvider currentUser; private final Clock clock;
	public ReportingService(EmployeeRepository employees,TrainingRepository trainings,TrainingAssignmentRepository assignments,TrainingCompletionRepository completions,CurrentUserProvider currentUser,Clock clock){this.employees=employees;this.trainings=trainings;this.assignments=assignments;this.completions=completions;this.currentUser=currentUser;this.clock=clock;}
	@Transactional(readOnly=true) public DashboardOverviewResponse overview(){requireAdmin(); LocalDate today=LocalDate.now(clock);return new DashboardOverviewResponse(employees.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,RegistrationStatus.ACTIVE),trainings.countByOrganizationId(DEFAULT_ORGANIZATION_ID),assignments.countByOrganizationId(DEFAULT_ORGANIZATION_ID),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.NOT_STARTED),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.IN_PROGRESS),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.COMPLETED),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.FAILED),completions.countExpired(DEFAULT_ORGANIZATION_ID,today),completions.countExpiring(DEFAULT_ORGANIZATION_ID,today,today.plusDays(30)),clock.instant());}
	@Transactional(readOnly=true) public DashboardOverviewResponse team(){var scope=new dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService(null,null,null); throw new UnsupportedOperationException("team scope is provided by the report query boundary");}
	@Transactional(readOnly=true) public PersonalDashboardResponse personal(){var u=currentUser.requireCurrentUser(); if(u.employeeId()==null)throw new AccessDeniedException("Usuário não está vinculado a colaborador."); UUID e=u.employeeId();return new PersonalDashboardResponse(new PersonalDashboardResponse.Counts(assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.NOT_STARTED),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.IN_PROGRESS),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.EXPIRING_SOON),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.EXPIRED),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.COMPLETED)));}
	private void requireAdmin(){if(currentUser.requireCurrentUser().role()!=dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN)throw new AccessDeniedException("Acesso restrito ao administrador.");}
}
