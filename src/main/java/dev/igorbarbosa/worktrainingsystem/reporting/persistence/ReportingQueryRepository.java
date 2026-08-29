package dev.igorbarbosa.worktrainingsystem.reporting.persistence;

import dev.igorbarbosa.worktrainingsystem.reporting.api.ActivityDashboardItem;
import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardFilter;
import dev.igorbarbosa.worktrainingsystem.reporting.api.DashboardOverviewResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.EmployeeDashboardItem;
import dev.igorbarbosa.worktrainingsystem.reporting.api.PersonalDashboardResponse;
import dev.igorbarbosa.worktrainingsystem.reporting.api.TrainingDashboardItem;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportingQueryRepository {
	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;

	public ReportingQueryRepository(NamedParameterJdbcTemplate jdbc, Clock clock) {
		this.jdbc = jdbc; this.clock = clock;
	}

	@Cacheable(cacheNames = "reporting-overview")
	public DashboardOverviewResponse overview(UUID organizationId, Set<UUID> allowedEmployees, DashboardFilter filter) {
		String scoped = scopedEmployeesSql(allowedEmployees, filter);
		String assignmentFilter = assignmentFilterSql(filter);
		String sql = """
			WITH scoped_employees AS (%s),
			filtered_assignments AS (
			  SELECT ta.* FROM training_assignments ta
			  JOIN scoped_employees e ON e.id = ta.employee_id
			  WHERE ta.organization_id = :organizationId %s
			)
			SELECT
			 (SELECT count(*) FROM scoped_employees) active_employees,
			 (SELECT count(*) FROM trainings t WHERE t.organization_id = :organizationId
			    AND (CAST(:trainingId AS uuid) IS NULL OR t.id = :trainingId)) registered_trainings,
			 count(*) assigned_trainings,
			 count(*) FILTER (WHERE status = 'NOT_STARTED') not_started,
			 count(*) FILTER (WHERE status IN ('IN_PROGRESS','AWAITING_ASSESSMENT','APPROVED')) in_progress,
			 count(*) FILTER (WHERE status = 'COMPLETED') completed,
			 count(*) FILTER (WHERE status = 'FAILED') failed,
			 count(*) FILTER (WHERE status = 'EXPIRED') expired,
			 count(*) FILTER (WHERE status = 'EXPIRING_SOON' OR due_date BETWEEN :today AND :thirtyDays) expiring,
			 count(DISTINCT employee_id) FILTER (WHERE status IN ('NOT_STARTED','IN_PROGRESS','AWAITING_ASSESSMENT','FAILED','EXPIRING_SOON','EXPIRED')) pending_employees,
			 (SELECT count(DISTINCT q.employee_id) FROM activity_qualifications q JOIN scoped_employees se ON se.id=q.employee_id
			    WHERE q.organization_id=:organizationId AND q.status='BLOCKED') blocked_employees
			FROM filtered_assignments
			""".formatted(scoped, assignmentFilter);
		MapSqlParameterSource parameters = parameters(organizationId, allowedEmployees, filter)
				.addValue("today", Date.valueOf(LocalDate.now(clock)))
				.addValue("thirtyDays", Date.valueOf(LocalDate.now(clock).plusDays(30)));
		return jdbc.queryForObject(sql, parameters, (rs, row) -> new DashboardOverviewResponse(
				rs.getLong("active_employees"), rs.getLong("registered_trainings"), rs.getLong("assigned_trainings"),
				rs.getLong("not_started"), rs.getLong("in_progress"), rs.getLong("completed"), rs.getLong("failed"),
				rs.getLong("expired"), rs.getLong("expiring"), rs.getLong("pending_employees"),
				rs.getLong("blocked_employees"), clock.instant()));
	}

	@Cacheable(cacheNames = "reporting-trainings")
	public QueryPage<TrainingDashboardItem> trainings(UUID organizationId, Set<UUID> allowedEmployees,
			DashboardFilter filter, int page, int size) {
		String scoped = scopedEmployeesSql(allowedEmployees, filter);
		String sql = """
			WITH scoped_employees AS (%s),
			filtered_assignments AS (
			 SELECT ta.* FROM training_assignments ta JOIN scoped_employees e ON e.id=ta.employee_id
			 WHERE ta.organization_id=:organizationId %s
			), latest_attempt AS (
			 SELECT assignment_id, score, result FROM (
			  SELECT aa.assignment_id, aa.score, aa.result,
			   row_number() OVER (PARTITION BY aa.assignment_id ORDER BY aa.submitted_at DESC, aa.id DESC) rn
			  FROM assessment_attempts aa WHERE aa.organization_id=:organizationId
			 ) ranked WHERE rn=1
			)
			SELECT t.id training_id, t.name training_name, t.code training_code,
			 count(fa.id) assigned,
			 count(fa.id) FILTER (WHERE fa.status='NOT_STARTED') not_started,
			 count(fa.id) FILTER (WHERE fa.status IN ('IN_PROGRESS','AWAITING_ASSESSMENT','APPROVED')) in_progress,
			 count(fa.id) FILTER (WHERE la.result='APPROVED') latest_approved,
			 count(fa.id) FILTER (WHERE la.result='FAILED') latest_failed,
			 count(fa.id) FILTER (WHERE fa.status='COMPLETED') completed,
			 count(fa.id) FILTER (WHERE fa.status='EXPIRED') expired,
			 round(CASE WHEN count(fa.id)=0 THEN 0 ELSE 100.0*count(fa.id) FILTER (WHERE fa.status='COMPLETED')/count(fa.id) END,2) completion_rate,
			 round(COALESCE(avg(la.score),0),2) average_assessment,
			 round(COALESCE(avg(EXTRACT(EPOCH FROM (tc.completed_at-fa.assigned_at))/3600.0),0),2) average_hours
			FROM trainings t
			LEFT JOIN filtered_assignments fa ON fa.training_id=t.id
			LEFT JOIN latest_attempt la ON la.assignment_id=fa.id
			LEFT JOIN training_completions tc ON tc.organization_id=:organizationId AND tc.source_assignment_id=fa.id
			WHERE t.organization_id=:organizationId AND (CAST(:trainingId AS uuid) IS NULL OR t.id=:trainingId)
			GROUP BY t.id,t.name,t.code
			ORDER BY t.name,t.id LIMIT :size OFFSET :offset
			""".formatted(scoped, assignmentFilterSql(filter));
		MapSqlParameterSource parameters = parameters(organizationId, allowedEmployees, filter)
				.addValue("size", size).addValue("offset", (long) page * size);
		List<TrainingDashboardItem> content = jdbc.query(sql, parameters, (rs, row) -> new TrainingDashboardItem(
				rs.getObject("training_id", UUID.class), rs.getString("training_name"), rs.getString("training_code"),
				rs.getLong("assigned"), rs.getLong("not_started"), rs.getLong("in_progress"),
				rs.getLong("latest_approved"), rs.getLong("latest_failed"), rs.getLong("completed"),
				rs.getLong("expired"), decimal(rs.getBigDecimal("completion_rate")),
				decimal(rs.getBigDecimal("average_assessment")), decimal(rs.getBigDecimal("average_hours"))));
		Long total = jdbc.queryForObject("SELECT count(*) FROM trainings WHERE organization_id=:organizationId AND (CAST(:trainingId AS uuid) IS NULL OR id=:trainingId)", parameters, Long.class);
		return new QueryPage<>(content, page, size, total == null ? 0 : total);
	}

	@Cacheable(cacheNames = "reporting-activities")
	public QueryPage<ActivityDashboardItem> activities(UUID organizationId, Set<UUID> allowedEmployees,
			DashboardFilter filter, int page, int size) {
		String scoped = scopedEmployeesSql(allowedEmployees, filter);
		String qualificationPeriod = "";
		if (filter.periodFrom() != null) qualificationPeriod += " AND q.calculated_at::date >= :periodFrom";
		if (filter.periodTo() != null) qualificationPeriod += " AND q.calculated_at::date <= :periodTo";
		String status = filter.status() == null ? "" : " AND q.status = :status";
		String sql = """
			WITH scoped_employees AS (%s)
			SELECT a.id activity_id,a.name activity_name,
			 (SELECT count(DISTINCT ja.job_id) FROM job_activities ja WHERE ja.organization_id=:organizationId AND ja.activity_id=a.id AND ja.status='ACTIVE') related_jobs,
			 (SELECT count(*) FROM activity_training_requirements r WHERE r.organization_id=:organizationId AND r.activity_id=a.id AND r.status='ACTIVE' AND (CAST(:trainingId AS uuid) IS NULL OR r.training_id=:trainingId)) requirements,
			 count(DISTINCT q.employee_id) FILTER (WHERE q.status='AVAILABLE') available_employees,
			 count(DISTINCT q.employee_id) FILTER (WHERE q.status='EXPIRING') expiring_employees,
			 count(DISTINCT q.employee_id) FILTER (WHERE q.status='BLOCKED') blocked_employees,
			 COALESCE((SELECT string_agg(blocker.name,'|' ORDER BY blocker.blocked DESC,blocker.name) FROM (
			   SELECT t.name,count(*) blocked FROM activity_training_requirements r
			   JOIN trainings t ON t.id=r.training_id AND t.organization_id=r.organization_id
			   JOIN training_assignments ta ON ta.training_id=r.training_id AND ta.organization_id=r.organization_id
			   JOIN scoped_employees se2 ON se2.id=ta.employee_id
			   WHERE r.organization_id=:organizationId AND r.activity_id=a.id AND r.status='ACTIVE'
			     AND ta.status IN ('NOT_STARTED','IN_PROGRESS','AWAITING_ASSESSMENT','FAILED','EXPIRING_SOON','EXPIRED')
			   GROUP BY t.id,t.name ORDER BY blocked DESC,t.name LIMIT 3
			 ) blocker),'') blockers
			FROM activities a
			LEFT JOIN activity_qualifications q ON q.activity_id=a.id AND q.organization_id=:organizationId
			 AND q.employee_id IN (SELECT id FROM scoped_employees) %s %s
			WHERE a.organization_id=:organizationId AND (CAST(:activityId AS uuid) IS NULL OR a.id=:activityId)
				 AND (CAST(:trainingId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM activity_training_requirements ar WHERE ar.organization_id=:organizationId AND ar.activity_id=a.id AND ar.training_id=:trainingId AND ar.status='ACTIVE'))
			GROUP BY a.id,a.name ORDER BY a.name,a.id LIMIT :size OFFSET :offset
			""".formatted(scoped, qualificationPeriod, status);
		MapSqlParameterSource parameters = parameters(organizationId, allowedEmployees, filter)
				.addValue("size", size).addValue("offset", (long) page * size);
		List<ActivityDashboardItem> content = jdbc.query(sql, parameters, (rs, row) -> new ActivityDashboardItem(
				rs.getObject("activity_id", UUID.class), rs.getString("activity_name"), rs.getLong("related_jobs"),
				rs.getLong("requirements"), rs.getLong("available_employees"), rs.getLong("expiring_employees"),
				rs.getLong("blocked_employees"), split(rs.getString("blockers"))));
		Long total = jdbc.queryForObject("SELECT count(*) FROM activities a WHERE a.organization_id=:organizationId AND (CAST(:activityId AS uuid) IS NULL OR a.id=:activityId) AND (CAST(:trainingId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM activity_training_requirements ar WHERE ar.organization_id=:organizationId AND ar.activity_id=a.id AND ar.training_id=:trainingId AND ar.status='ACTIVE'))", parameters, Long.class);
		return new QueryPage<>(content, page, size, total == null ? 0 : total);
	}

	@Cacheable(cacheNames = "reporting-employees")
	public QueryPage<EmployeeDashboardItem> employees(UUID organizationId, Set<UUID> allowedEmployees,
			DashboardFilter filter, int page, int size) {
		String scoped = scopedEmployeesSql(allowedEmployees, filter);
		String assignmentExists = employeeAssignmentExistsSql(filter);
		String sql = """
			WITH scoped_employees AS (%s), latest_attempt AS (
			 SELECT employee_id,score FROM (
			  SELECT aa.employee_id,aa.assignment_id,aa.score,row_number() OVER(PARTITION BY aa.assignment_id ORDER BY aa.submitted_at DESC,aa.id DESC) rn
			  FROM assessment_attempts aa WHERE aa.organization_id=:organizationId
			 ) ranked WHERE rn=1
			)
			SELECT e.id employee_id,e.name employee_name,e.registration,e.unit_id,u.name unit_name,
			 e.sector_id,s.name sector_name,e.job_id,j.name job_name,
			 (SELECT count(DISTINCT r.training_id) FROM employee_activities ea JOIN activity_training_requirements r ON r.organization_id=ea.organization_id AND r.activity_id=ea.activity_id AND r.status='ACTIVE' WHERE ea.organization_id=:organizationId AND ea.employee_id=e.id AND ea.status='ACTIVE') mandatory_trainings,
			 (SELECT count(DISTINCT ta.training_id) FROM training_assignments ta WHERE ta.organization_id=:organizationId AND ta.employee_id=e.id AND NOT EXISTS (SELECT 1 FROM employee_activities ea JOIN activity_training_requirements r ON r.organization_id=ea.organization_id AND r.activity_id=ea.activity_id AND r.status='ACTIVE' WHERE ea.organization_id=:organizationId AND ea.employee_id=e.id AND ea.status='ACTIVE' AND r.training_id=ta.training_id)) optional_trainings,
			 COALESCE((SELECT round(avg(progress),2) FROM (SELECT CASE WHEN ta.status='COMPLETED' THEN 100 ELSE COALESCE(avg(vp.percentage_watched),0) END progress FROM training_assignments ta LEFT JOIN video_progress vp ON vp.organization_id=ta.organization_id AND vp.assignment_id=ta.id WHERE ta.organization_id=:organizationId AND ta.employee_id=e.id GROUP BY ta.id,ta.status) p),0) average_progress,
			 COALESCE((SELECT round(avg(la.score),2) FROM latest_attempt la WHERE la.employee_id=e.id),0) average_assessment,
			 (SELECT count(*) FROM training_completions tc WHERE tc.organization_id=:organizationId AND tc.employee_id=e.id) completions,
			 (SELECT count(*) FROM training_completions tc WHERE tc.organization_id=:organizationId AND tc.employee_id=e.id AND tc.expiration_date IS NOT NULL) expirations,
			 (SELECT count(*) FROM activity_qualifications q WHERE q.organization_id=:organizationId AND q.employee_id=e.id AND q.status='AVAILABLE') available_activities,
			 (SELECT count(*) FROM activity_qualifications q WHERE q.organization_id=:organizationId AND q.employee_id=e.id AND q.status='BLOCKED') blocked_activities
			FROM scoped_employees e JOIN units u ON u.id=e.unit_id JOIN sectors s ON s.id=e.sector_id JOIN jobs j ON j.id=e.job_id
			WHERE 1=1 %s ORDER BY e.name,e.id LIMIT :size OFFSET :offset
			""".formatted(scoped, assignmentExists);
		MapSqlParameterSource parameters = parameters(organizationId, allowedEmployees, filter)
				.addValue("size", size).addValue("offset", (long) page * size);
		List<EmployeeDashboardItem> content = jdbc.query(sql, parameters, (rs, row) -> new EmployeeDashboardItem(
				rs.getObject("employee_id", UUID.class), rs.getString("employee_name"), rs.getString("registration"),
				rs.getObject("unit_id", UUID.class), rs.getString("unit_name"), rs.getObject("sector_id", UUID.class),
				rs.getString("sector_name"), rs.getObject("job_id", UUID.class), rs.getString("job_name"),
				rs.getLong("mandatory_trainings"), rs.getLong("optional_trainings"),
				decimal(rs.getBigDecimal("average_progress")), decimal(rs.getBigDecimal("average_assessment")),
				rs.getLong("completions"), rs.getLong("expirations"), rs.getLong("available_activities"),
				rs.getLong("blocked_activities")));
		String countSql = "WITH scoped_employees AS (" + scoped + ") SELECT count(*) FROM scoped_employees e WHERE 1=1 " + assignmentExists;
		Long total = jdbc.queryForObject(countSql, parameters, Long.class);
		return new QueryPage<>(content, page, size, total == null ? 0 : total);
	}

	@Cacheable(cacheNames = "reporting-personal")
	public PersonalDashboardResponse personal(UUID organizationId, UUID employeeId) {
		MapSqlParameterSource p = new MapSqlParameterSource("organizationId", organizationId).addValue("employeeId", employeeId);
		PersonalDashboardResponse.Counts counts = jdbc.queryForObject("""
			SELECT count(*) FILTER (WHERE status='NOT_STARTED') pending,
			 count(*) FILTER (WHERE status IN ('IN_PROGRESS','AWAITING_ASSESSMENT','APPROVED')) in_progress,
			 count(*) FILTER (WHERE status='EXPIRING_SOON') expiring,
			 count(*) FILTER (WHERE status='EXPIRED') expired,
			 count(*) FILTER (WHERE status='COMPLETED') completed,
			 (SELECT count(*) FROM activity_qualifications q WHERE q.organization_id=:organizationId AND q.employee_id=:employeeId AND q.status='AVAILABLE') available_activities,
			 (SELECT count(*) FROM activity_qualifications q WHERE q.organization_id=:organizationId AND q.employee_id=:employeeId AND q.status='BLOCKED') blocked_activities
			FROM training_assignments WHERE organization_id=:organizationId AND employee_id=:employeeId
			""", p, (rs,row) -> new PersonalDashboardResponse.Counts(rs.getLong("pending"),rs.getLong("in_progress"),
				rs.getLong("expiring"),rs.getLong("expired"),rs.getLong("completed"),rs.getLong("available_activities"),
				rs.getLong("blocked_activities")));
		List<PersonalDashboardResponse.ContinueTraining> continuing = jdbc.query("""
			SELECT ta.id,t.name,COALESCE(avg(vp.percentage_watched),0) progress,
			 (array_agg(vp.video_id ORDER BY vp.updated_at DESC) FILTER (WHERE vp.video_id IS NOT NULL))[1] video_id,
			 COALESCE((array_agg(vp.position_seconds ORDER BY vp.updated_at DESC) FILTER (WHERE vp.video_id IS NOT NULL))[1],0) position_seconds
			FROM training_assignments ta JOIN trainings t ON t.id=ta.training_id AND t.organization_id=ta.organization_id
			LEFT JOIN video_progress vp ON vp.organization_id=ta.organization_id AND vp.assignment_id=ta.id
			WHERE ta.organization_id=:organizationId AND ta.employee_id=:employeeId AND ta.status IN ('IN_PROGRESS','AWAITING_ASSESSMENT','APPROVED')
			GROUP BY ta.id,t.name ORDER BY ta.updated_at DESC LIMIT 1
			""", p, (rs,row) -> new PersonalDashboardResponse.ContinueTraining(rs.getObject("id",UUID.class),rs.getString("name"),
				decimal(rs.getBigDecimal("progress")), rs.getObject("video_id") == null ? null : new PersonalDashboardResponse.ResumeAt(rs.getObject("video_id",UUID.class),rs.getLong("position_seconds"))));
		List<PersonalDashboardResponse.TrainingSummary> pending = trainingSummaries(p, "('NOT_STARTED','IN_PROGRESS','AWAITING_ASSESSMENT','FAILED')", "ta.due_date NULLS LAST,ta.assigned_at DESC");
		List<PersonalDashboardResponse.TrainingSummary> expiring = trainingSummaries(p, "('EXPIRING_SOON','EXPIRED')", "ta.due_date,ta.assigned_at DESC");
		List<PersonalDashboardResponse.ActivitySummary> blocked = jdbc.query("""
			SELECT q.activity_id,a.name,q.status,COALESCE(string_agg(DISTINCT reason->>'trainingName','|' ORDER BY reason->>'trainingName') FILTER (WHERE reason->>'trainingName' IS NOT NULL),'') blockers
			FROM activity_qualifications q JOIN activities a ON a.id=q.activity_id AND a.organization_id=q.organization_id
			LEFT JOIN LATERAL jsonb_array_elements(q.blocking_reasons) reason ON true
			WHERE q.organization_id=:organizationId AND q.employee_id=:employeeId AND q.status='BLOCKED'
			GROUP BY q.activity_id,a.name,q.status ORDER BY a.name LIMIT 5
			""", p, (rs,row) -> new PersonalDashboardResponse.ActivitySummary(rs.getObject("activity_id",UUID.class),
				rs.getString("name"),rs.getString("status"),split(rs.getString("blockers"))));
		return new PersonalDashboardResponse(continuing.stream().findFirst().orElse(null), counts, pending, expiring, blocked);
	}

	private List<PersonalDashboardResponse.TrainingSummary> trainingSummaries(MapSqlParameterSource p, String statuses, String order) {
		String sql = """
			SELECT ta.id assignment_id,ta.training_id,t.name,ta.status,ta.due_date,COALESCE(avg(vp.percentage_watched),0) progress
			FROM training_assignments ta JOIN trainings t ON t.id=ta.training_id AND t.organization_id=ta.organization_id
			LEFT JOIN video_progress vp ON vp.organization_id=ta.organization_id AND vp.assignment_id=ta.id
			WHERE ta.organization_id=:organizationId AND ta.employee_id=:employeeId AND ta.status IN %s
			GROUP BY ta.id,ta.training_id,t.name,ta.status,ta.due_date,ta.assigned_at ORDER BY %s LIMIT 5
			""".formatted(statuses, order);
		return jdbc.query(sql,p,(rs,row)->new PersonalDashboardResponse.TrainingSummary(rs.getObject("assignment_id",UUID.class),
				rs.getObject("training_id",UUID.class),rs.getString("name"),rs.getString("status"),
				rs.getDate("due_date") == null ? null : rs.getDate("due_date").toLocalDate(),decimal(rs.getBigDecimal("progress"))));
	}

	private String scopedEmployeesSql(Set<UUID> allowedEmployees, DashboardFilter filter) {
		StringBuilder sql = new StringBuilder("SELECT e.* FROM employees e WHERE e.organization_id=:organizationId AND e.status='ACTIVE'");
		if (allowedEmployees != null) sql.append(allowedEmployees.isEmpty() ? " AND 1=0" : " AND e.id IN (:allowedEmployees)");
		if (filter.unitId() != null) sql.append(" AND e.unit_id=:unitId");
		if (filter.sectorId() != null) sql.append(" AND e.sector_id=:sectorId");
		if (filter.jobId() != null) sql.append(" AND e.job_id=:jobId");
		if (filter.activityId() != null) sql.append(" AND EXISTS (SELECT 1 FROM employee_activities ea WHERE ea.organization_id=e.organization_id AND ea.employee_id=e.id AND ea.activity_id=:activityId AND ea.status='ACTIVE')");
		return sql.toString();
	}

	private String assignmentFilterSql(DashboardFilter filter) {
		StringBuilder sql = new StringBuilder();
		if (filter.trainingId() != null) sql.append(" AND ta.training_id=:trainingId");
		if (filter.status() != null) sql.append(" AND ta.status=:status");
		if (filter.periodFrom() != null) sql.append(" AND ta.assigned_date>=:periodFrom");
		if (filter.periodTo() != null) sql.append(" AND ta.assigned_date<=:periodTo");
		return sql.toString();
	}

	private String employeeAssignmentExistsSql(DashboardFilter filter) {
		if (filter.trainingId() == null && filter.status() == null && filter.periodFrom() == null && filter.periodTo() == null) return "";
		return " AND EXISTS (SELECT 1 FROM training_assignments ta WHERE ta.organization_id=:organizationId AND ta.employee_id=e.id" + assignmentFilterSql(filter) + ")";
	}

	private MapSqlParameterSource parameters(UUID organizationId, Set<UUID> allowedEmployees, DashboardFilter filter) {
		MapSqlParameterSource p = new MapSqlParameterSource("organizationId", organizationId)
				.addValue("allowedEmployees", allowedEmployees == null ? List.of() : allowedEmployees)
				.addValue("unitId", filter.unitId()).addValue("sectorId", filter.sectorId())
				.addValue("jobId", filter.jobId()).addValue("activityId", filter.activityId())
				.addValue("trainingId", filter.trainingId()).addValue("status", filter.status())
				.addValue("periodFrom", filter.periodFrom() == null ? null : Date.valueOf(filter.periodFrom()))
				.addValue("periodTo", filter.periodTo() == null ? null : Date.valueOf(filter.periodTo()));
		return p;
	}

	private BigDecimal decimal(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
	private List<String> split(String value) {
		return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split("\\|"))
				.filter(item -> !item.isBlank()).toList();
	}

	public record QueryPage<T>(List<T> content, int page, int size, long totalElements) {
		public int totalPages() { return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size); }
	}
}
