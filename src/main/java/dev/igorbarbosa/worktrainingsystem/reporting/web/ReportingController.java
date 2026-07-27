package dev.igorbarbosa.worktrainingsystem.reporting.web;

import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardOverviewResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.PersonalDashboardResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.application.ReportingService;
import dev.igorbarbosa.worktrainingsystem.assignments.application.AssignmentService;
import dev.igorbarbosa.worktrainingsystem.assignments.api.AssignmentResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationService;
import dev.igorbarbosa.worktrainingsystem.qualifications.api.QualificationResponse;
import dev.igorbarbosa.worktrainingsystem.qualifications.domain.QualificationStatus;
import dev.igorbarbosa.worktrainingsystem.expirations.application.ExpirationService;
import dev.igorbarbosa.worktrainingsystem.expirations.api.ExpirationResponse;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeService;
import dev.igorbarbosa.worktrainingsystem.employees.api.EmployeeResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1") public class ReportingController {
	private final ReportingService service; private final AssignmentService assignments; private final QualificationService qualifications; private final ExpirationService expirations; private final EmployeeService employees; private final PaginationFactory pagination;
	public ReportingController(ReportingService service, AssignmentService assignments, QualificationService qualifications, ExpirationService expirations, EmployeeService employees, PaginationFactory pagination){this.service=service;this.assignments=assignments;this.qualifications=qualifications;this.expirations=expirations;this.employees=employees;this.pagination=pagination;}
	@GetMapping("/me/dashboard") @PreAuthorize("hasRole('EMPLOYEE')") public PersonalDashboardResponse personal(){return service.personal();}
	@GetMapping("/admin/dashboard/overview") @PreAuthorize("hasRole('ADMIN')") public DashboardOverviewResponse overview(){return service.overview();}
	@GetMapping("/reports/training-status") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')") public PageResponse<AssignmentResponse> trainingStatus(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return PageResponse.from(assignments.list(null,null,null,null,null,null,pagination.create(page,size,"assignedAt,desc",Set.of("assignedAt","dueDate","status"))));}
	@GetMapping("/reports/qualifications") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')") public PageResponse<QualificationResponse> qualifications(@RequestParam(required=false) QualificationStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return PageResponse.from(qualifications.list(null,null,status,pagination.create(page,size,"calculatedAt,desc",Set.of("calculatedAt","status"))));}
	@GetMapping("/reports/expirations") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPERVISOR')") public PageResponse<ExpirationResponse> expirations(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return PageResponse.from(expirations.list(null,null,null,null,null,null,null,null,pagination.create(page,size,"expirationDate,asc",Set.of("expirationDate","completionDate"))));}
	@GetMapping("/admin/dashboard/trainings") @PreAuthorize("hasRole('ADMIN')") public DashboardOverviewResponse trainings(){return service.overview();}
	@GetMapping("/admin/dashboard/activities") @PreAuthorize("hasRole('ADMIN')") public DashboardOverviewResponse activities(){return service.overview();}
	@GetMapping("/admin/dashboard/employees") @PreAuthorize("hasRole('ADMIN')") public DashboardOverviewResponse employees(){return service.overview();}
	@GetMapping("/team/dashboard") @PreAuthorize("hasAnyRole('MANAGER','SUPERVISOR')") public DashboardOverviewResponse team(){return service.team();}
	@GetMapping("/team/employees") @PreAuthorize("hasAnyRole('MANAGER','SUPERVISOR')") public PageResponse<EmployeeResponse> teamEmployees(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return PageResponse.from(employees.list(null,null,null,null,null,null,null,pagination.create(page,size,"createdAt,desc",Set.of("createdAt","name","registration","status"))));}
}
