package dev.igorbarbosa.worktrainingsystem.reporting.application;

import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.reporting.api.ActivityDashboardItem;
import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardFilter;
import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardOverviewResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.EmployeeDashboardItem;
import dev.igorbarbosa.worktrainingsystem.reporting.api.PersonalDashboardResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.TrainingDashboardItem;
import dev.igorbarbosa.worktrainingsystem.reporting.persistence.ReportingQueryRepository;
import dev.igorbarbosa.worktrainingsystem.reporting.persistence.ReportingQueryRepository.QueryPage;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PageResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
	private final ReportingQueryRepository queries;
	private final CurrentUserProvider currentUser;
	private final AuthorizationService authorization;

	public ReportingService(ReportingQueryRepository queries, CurrentUserProvider currentUser,
			AuthorizationService authorization) {
		this.queries = queries; this.currentUser = currentUser; this.authorization = authorization;
	}

	@Transactional(readOnly = true)
	public DashboardOverviewResponse overview(DashboardFilter filter) {
		CurrentUser actor = requireAdmin();
		return queries.overview(actor.organizationId(), null, filter);
	}

	@Transactional(readOnly = true)
	public PageResponse<TrainingDashboardItem> trainings(DashboardFilter filter, int page, int size) {
		CurrentUser actor = requireAdmin();
		return page(queries.trainings(actor.organizationId(), null, filter, page, size));
	}

	@Transactional(readOnly = true)
	public PageResponse<ActivityDashboardItem> activities(DashboardFilter filter, int page, int size) {
		CurrentUser actor = requireAdmin();
		return page(queries.activities(actor.organizationId(), null, filter, page, size));
	}

	@Transactional(readOnly = true)
	public PageResponse<EmployeeDashboardItem> employees(DashboardFilter filter, int page, int size) {
		CurrentUser actor = requireAdmin();
		return page(queries.employees(actor.organizationId(), null, filter, page, size));
	}

	@Transactional(readOnly = true)
	public DashboardOverviewResponse team(DashboardFilter filter) {
		var scope = authorization.currentScope();
		if (!scope.manager()) throw new AccessDeniedException("Acesso restrito ao gestor ou supervisor.");
		Set<UUID> employeeIds = authorization.scopeReferences(scope).employeeIds();
		return queries.overview(scope.organizationId(), employeeIds, filter);
	}

	@Transactional(readOnly = true)
	public PersonalDashboardResponse personal() {
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.employeeId() == null) throw new AccessDeniedException("Usuário não está vinculado a colaborador.");
		return queries.personal(actor.organizationId(), actor.employeeId());
	}

	private CurrentUser requireAdmin() {
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.role() != UserRole.ADMIN) throw new AccessDeniedException("Acesso restrito ao administrador.");
		return actor;
	}

	private <T> PageResponse<T> page(QueryPage<T> value) {
		int totalPages = value.totalPages();
		return new PageResponse<>(value.content(), value.page(), value.size(), value.totalElements(), totalPages,
				value.page() == 0, totalPages == 0 || value.page() + 1 >= totalPages, List.of());
	}
}
