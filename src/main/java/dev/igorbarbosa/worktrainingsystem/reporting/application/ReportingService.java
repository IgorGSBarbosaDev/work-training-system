package dev.igorbarbosa.worktrainingsystem.reporting.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.assessments.persistence.TrainingCompletionRepository;
import dev.igorbarbosa.worktrainingsystem.employees.persistence.EmployeeRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
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
	private final EmployeeRepository employees; private final TrainingRepository trainings; private final TrainingAssignmentRepository assignments; private final TrainingCompletionRepository completions; private final CurrentUserProvider currentUser; private final AuthorizationService authorization; private final Clock clock;
	public ReportingService(EmployeeRepository employees,TrainingRepository trainings,TrainingAssignmentRepository assignments,TrainingCompletionRepository completions,CurrentUserProvider currentUser,AuthorizationService authorization,Clock clock){this.employees=employees;this.trainings=trainings;this.assignments=assignments;this.completions=completions;this.currentUser=currentUser;this.authorization=authorization;this.clock=clock;}
	@Transactional(readOnly=true) public DashboardOverviewResponse overview(){requireAdmin(); LocalDate today=LocalDate.now(clock);return new DashboardOverviewResponse(employees.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,RegistrationStatus.ACTIVE),trainings.countByOrganizationId(DEFAULT_ORGANIZATION_ID),assignments.countByOrganizationId(DEFAULT_ORGANIZATION_ID),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.NOT_STARTED),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.IN_PROGRESS),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.COMPLETED),assignments.countByOrganizationIdAndStatus(DEFAULT_ORGANIZATION_ID,AssignmentStatus.FAILED),completions.countExpired(DEFAULT_ORGANIZATION_ID,today),completions.countExpiring(DEFAULT_ORGANIZATION_ID,today,today.plusDays(30)),clock.instant());}
	@Transactional(readOnly=true) public DashboardOverviewResponse team(){
		var scope = authorization.currentScope();
		if (!scope.manager()) throw new AccessDeniedException("Acesso restrito ao gestor ou supervisor.");
		var employeeIds = authorization.scopeReferences(scope).employeeIds();
		if (employeeIds.isEmpty()) return emptyTeam();
		LocalDate today = LocalDate.now(clock);
		return new DashboardOverviewResponse(
			employees.countByOrganizationIdAndIdIn(scope.organizationId(), employeeIds),
			trainings.countByOrganizationId(scope.organizationId()),
			assignments.countByOrganizationIdAndEmployeeIdIn(scope.organizationId(), employeeIds),
			assignments.countByOrganizationIdAndEmployeeIdInAndStatus(scope.organizationId(), employeeIds, AssignmentStatus.NOT_STARTED),
			assignments.countByOrganizationIdAndEmployeeIdInAndStatus(scope.organizationId(), employeeIds, AssignmentStatus.IN_PROGRESS),
			assignments.countByOrganizationIdAndEmployeeIdInAndStatus(scope.organizationId(), employeeIds, AssignmentStatus.COMPLETED),
			assignments.countByOrganizationIdAndEmployeeIdInAndStatus(scope.organizationId(), employeeIds, AssignmentStatus.FAILED),
			completions.countExpiredForEmployees(scope.organizationId(), employeeIds, today),
			completions.countExpiringForEmployees(scope.organizationId(), employeeIds, today, today.plusDays(30)),
			clock.instant());
	}
	private DashboardOverviewResponse emptyTeam(){return new DashboardOverviewResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, clock.instant());}
	@Transactional(readOnly=true) public PersonalDashboardResponse personal(){var u=currentUser.requireCurrentUser(); if(u.employeeId()==null)throw new AccessDeniedException("Usuário não está vinculado a colaborador."); UUID e=u.employeeId();return new PersonalDashboardResponse(new PersonalDashboardResponse.Counts(assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.NOT_STARTED),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.IN_PROGRESS),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.EXPIRING_SOON),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.EXPIRED),assignments.countByOrganizationIdAndEmployeeIdAndStatus(DEFAULT_ORGANIZATION_ID,e,AssignmentStatus.COMPLETED)));}
	private void requireAdmin(){if(currentUser.requireCurrentUser().role()!=dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole.ADMIN)throw new AccessDeniedException("Acesso restrito ao administrador.");}
}
