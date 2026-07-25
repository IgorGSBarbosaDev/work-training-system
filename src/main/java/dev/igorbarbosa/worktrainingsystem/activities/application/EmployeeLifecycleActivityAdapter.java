package dev.igorbarbosa.worktrainingsystem.activities.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivity;
import dev.igorbarbosa.worktrainingsystem.activities.domain.EmployeeActivityOrigin;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.ActivityTrainingRequirementRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.EmployeeActivityRepository;
import dev.igorbarbosa.worktrainingsystem.activities.persistence.JobActivityRepository;
import dev.igorbarbosa.worktrainingsystem.employees.application.EmployeeLifecyclePort;
import dev.igorbarbosa.worktrainingsystem.qualifications.application.QualificationCommandPort;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class EmployeeLifecycleActivityAdapter implements EmployeeLifecyclePort {
	private final JobActivityRepository jobActivities;
	private final EmployeeActivityRepository employeeActivities;
	private final ActivityTrainingRequirementRepository requirements;
	private final AssignmentGenerationPort assignments;
	private final QualificationCommandPort qualifications;
	EmployeeLifecycleActivityAdapter(JobActivityRepository jobActivities, EmployeeActivityRepository employeeActivities,
			ActivityTrainingRequirementRepository requirements, AssignmentGenerationPort assignments,
			QualificationCommandPort qualifications) {
		this.jobActivities = jobActivities; this.employeeActivities = employeeActivities; this.requirements = requirements;
		this.assignments = assignments; this.qualifications = qualifications;
	}

	@Override @Transactional
	public LifecycleEffects initialize(EmployeeData employee, UUID actor) {
		if (!employee.active()) return LifecycleEffects.none();
		return apply(employee, null, false, actor);
	}

	@Override @Transactional
	public LifecycleEffects changeJob(EmployeeData employee, UUID previousJobId,
			boolean removePreviousJobActivities, UUID actor) {
		if (!employee.active()) return LifecycleEffects.none();
		return apply(employee, previousJobId, removePreviousJobActivities, actor);
	}

	private LifecycleEffects apply(EmployeeData employee, UUID previousJobId, boolean removePrevious, UUID actor) {
		Set<UUID> before = effectiveActivityIds(employee.id()); Instant now = Instant.now();
		if (removePrevious && previousJobId != null) {
			for (var source : jobActivities.findAllByOrganizationIdAndJobIdAndStatusOrderByLinkedAt(
					DEFAULT_ORGANIZATION_ID, previousJobId, RegistrationStatus.ACTIVE)) {
				employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndSourceJobActivityIdAndStatus(
						DEFAULT_ORGANIZATION_ID, employee.id(), source.getActivityId(), source.getId(), RegistrationStatus.ACTIVE)
						.ifPresent(link -> link.deactivate(actor, now));
			}
		}
		Set<UUID> newJobActivities = new HashSet<>(); int assignmentsCreated = 0;
		for (var source : jobActivities.findAllByOrganizationIdAndJobIdAndStatusOrderByLinkedAt(
				DEFAULT_ORGANIZATION_ID, employee.jobId(), RegistrationStatus.ACTIVE)) {
			newJobActivities.add(source.getActivityId());
			if (employeeActivities.findByOrganizationIdAndEmployeeIdAndActivityIdAndSourceJobActivityIdAndStatus(
					DEFAULT_ORGANIZATION_ID, employee.id(), source.getActivityId(), source.getId(), RegistrationStatus.ACTIVE).isEmpty()) {
				employeeActivities.saveAndFlush(new EmployeeActivity(DEFAULT_ORGANIZATION_ID, employee.id(),
						source.getActivityId(), EmployeeActivityOrigin.JOB, source.getId(), null, actor, now));
			}
			for (var requirement : requirements.findAllByOrganizationIdAndActivityIdAndStatusOrderByLinkedAt(
					DEFAULT_ORGANIZATION_ID, source.getActivityId(), RegistrationStatus.ACTIVE)) {
				assignmentsCreated += assignments.generate(new ActivityAssignmentRequested(DEFAULT_ORGANIZATION_ID,
						Set.of(employee.id()), source.getActivityId(), requirement.getId(), requirement.getTrainingId(),
						requirement.getVersionPolicy(), requirement.getTrainingVersionId(), actor));
			}
		}
		Set<UUID> after = effectiveActivityIds(employee.id());
		Set<UUID> added = new HashSet<>(after); added.removeAll(before);
		Set<UUID> removed = new HashSet<>(before); removed.removeAll(after);
		int recalculated = qualifications.recalculateEmployee(employee.id());
		return new LifecycleEffects(added.size(), removed.size(), assignmentsCreated, recalculated);
	}

	private Set<UUID> effectiveActivityIds(UUID employeeId) {
		Set<UUID> ids = new HashSet<>();
		for (var link : employeeActivities.findAllByOrganizationIdAndEmployeeIdAndStatusOrderByAssignedAt(
				DEFAULT_ORGANIZATION_ID, employeeId, RegistrationStatus.ACTIVE)) ids.add(link.getActivityId());
		return ids;
	}
}
