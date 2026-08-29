package dev.igorbarbosa.worktrainingsystem.notifications.application;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssignmentDueNotificationJob {
	private static final Set<AssignmentStatus> PENDING = Set.of(AssignmentStatus.NOT_STARTED,
			AssignmentStatus.IN_PROGRESS, AssignmentStatus.AWAITING_ASSESSMENT, AssignmentStatus.FAILED);
	private final TrainingAssignmentRepository assignments;
	private final SliceBNotificationPort notifications;
	private final Clock clock;
	private final int windowDays;

	public AssignmentDueNotificationJob(TrainingAssignmentRepository assignments, SliceBNotificationPort notifications,
			Clock clock, @Value("${app.notifications.due-soon-days:7}") int windowDays) {
		this.assignments = assignments; this.notifications = notifications; this.clock = clock;
		this.windowDays = Math.max(1, windowDays);
	}

	@Scheduled(cron = "${app.notifications.due-soon-cron:0 30 2 * * *}", zone = "UTC")
	public void notifyAssignmentsDueSoon() {
		LocalDate today = LocalDate.now(clock);
		int page = 0;
		Page<TrainingAssignment> result;
		do {
			result = assignments.findAllByStatusInAndDueDateBetween(PENDING, today, today.plusDays(windowDays),
					PageRequest.of(page++, 500));
			result.forEach(assignment -> notifications.assignmentDue(new SliceBNotificationPort.AssignmentNotification(
					assignment.getOrganizationId(), assignment.getEmployeeId(), assignment.getId(),
					assignment.getTrainingId(), assignment.getDueDate())));
		} while (result.hasNext());
	}
}
