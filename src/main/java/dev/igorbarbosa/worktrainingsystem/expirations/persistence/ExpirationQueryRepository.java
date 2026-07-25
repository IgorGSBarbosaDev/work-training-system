package dev.igorbarbosa.worktrainingsystem.expirations.persistence;

import dev.igorbarbosa.worktrainingsystem.assessments.domain.TrainingCompletion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface ExpirationQueryRepository extends Repository<TrainingCompletion, UUID> {
	@Query(value = """
			select c.id as completionId, c.employee_id as employeeId, c.training_id as trainingId,
			 c.completion_date as completionDate,
			 case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end as expirationDate,
			 case when (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) < :today
			      then 'EXPIRED' else 'EXPIRING_SOON' end as status
			from training_completions c
			join employees e on e.id = c.employee_id and e.organization_id = c.organization_id
			left join lateral (select x.completion_id, x.recalculated_expiration_date from completion_expiration_history x
			 where x.organization_id = c.organization_id and x.completion_id = c.id
			 order by x.created_at desc, x.id desc limit 1) h on true
			where c.organization_id = :organizationId
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) is not null
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) <= :windowEnd
			 and (:employeeId is null or c.employee_id = :employeeId)
			 and (:trainingId is null or c.training_id = :trainingId)
			 and (:unitId is null or e.unit_id = :unitId)
			 and (:sectorId is null or e.sector_id = :sectorId)
			 and (:jobId is null or e.job_id = :jobId)
			 and (:expiresFrom is null or (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) >= :expiresFrom)
			 and (:expiresTo is null or (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) <= :expiresTo)
			 and (:status is null or :status = case when (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) < :today
			      then 'EXPIRED' else 'EXPIRING_SOON' end)
			 and (:admin = true or c.employee_id in (:allowedEmployeeIds))
			""", countQuery = """
			select count(*) from training_completions c
			join employees e on e.id = c.employee_id and e.organization_id = c.organization_id
			left join lateral (select x.completion_id, x.recalculated_expiration_date from completion_expiration_history x
			 where x.organization_id = c.organization_id and x.completion_id = c.id
			 order by x.created_at desc, x.id desc limit 1) h on true
			where c.organization_id = :organizationId
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) is not null
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) <= :windowEnd
			 and (:employeeId is null or c.employee_id = :employeeId)
			 and (:trainingId is null or c.training_id = :trainingId)
			 and (:unitId is null or e.unit_id = :unitId) and (:sectorId is null or e.sector_id = :sectorId)
			 and (:jobId is null or e.job_id = :jobId)
			 and (:expiresFrom is null or (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) >= :expiresFrom)
			 and (:expiresTo is null or (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) <= :expiresTo)
			 and (:status is null or :status = case when (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) < :today
			      then 'EXPIRED' else 'EXPIRING_SOON' end)
			 and (:admin = true or c.employee_id in (:allowedEmployeeIds))
			""", nativeQuery = true)
	Page<ExpirationView> findExpirations(UUID organizationId, LocalDate today, LocalDate windowEnd,
			UUID employeeId, UUID trainingId, UUID unitId, UUID sectorId, UUID jobId, String status,
			LocalDate expiresFrom, LocalDate expiresTo, boolean admin, Collection<UUID> allowedEmployeeIds,
			Pageable pageable);

	@Query(value = """
			select c.id as completionId, c.employee_id as employeeId, c.training_id as trainingId,
			 case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end as expirationDate
			from training_completions c
			left join lateral (select x.completion_id, x.recalculated_expiration_date from completion_expiration_history x
			 where x.organization_id = c.organization_id and x.completion_id = c.id
			 order by x.created_at desc, x.id desc limit 1) h on true
			where c.organization_id = :organizationId
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) is not null
			 and (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end) <= :windowEnd
			order by (case when h.completion_id is not null then h.recalculated_expiration_date else c.expiration_date end), c.id
			""", nativeQuery = true)
	java.util.List<ExpirationCandidate> findCandidates(UUID organizationId, LocalDate windowEnd);

	interface ExpirationView {
		UUID getCompletionId(); UUID getEmployeeId(); UUID getTrainingId(); LocalDate getCompletionDate();
		LocalDate getExpirationDate(); String getStatus();
	}
	interface ExpirationCandidate {
		UUID getCompletionId(); UUID getEmployeeId(); UUID getTrainingId(); LocalDate getExpirationDate();
	}
}
