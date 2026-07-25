package dev.igorbarbosa.worktrainingsystem.assignments.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatus;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatusEvent;
import dev.igorbarbosa.worktrainingsystem.assignments.domain.TrainingAssignment;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.AssignmentStatusEventRepository;
import dev.igorbarbosa.worktrainingsystem.assignments.persistence.TrainingAssignmentRepository;
import dev.igorbarbosa.worktrainingsystem.identity.application.AuthorizationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUser;
import dev.igorbarbosa.worktrainingsystem.identity.application.CurrentUserProvider;
import dev.igorbarbosa.worktrainingsystem.identity.domain.UserRole;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceConflictException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentExecutionService implements AssignmentExecutionPort {
	private static final EnumSet<AssignmentStatus> PLAYABLE = EnumSet.of(AssignmentStatus.NOT_STARTED,
			AssignmentStatus.IN_PROGRESS, AssignmentStatus.AWAITING_ASSESSMENT, AssignmentStatus.APPROVED,
			AssignmentStatus.FAILED);
	private final TrainingAssignmentRepository assignments;
	private final AssignmentStatusEventRepository statusEvents;
	private final AuthorizationService authorization;
	private final CurrentUserProvider currentUser;
	private final TrainingReadinessPort readiness;
	private final Clock clock;

	public AssignmentExecutionService(TrainingAssignmentRepository assignments, AssignmentStatusEventRepository statusEvents,
			AuthorizationService authorization, CurrentUserProvider currentUser, TrainingReadinessPort readiness, Clock clock) {
		this.assignments = assignments; this.statusEvents = statusEvents; this.authorization = authorization;
		this.currentUser = currentUser; this.readiness = readiness; this.clock = clock;
	}

	@Override @Transactional(readOnly = true)
	public ExecutionAssignment view(UUID assignmentId) {
		TrainingAssignment assignment = require(assignmentId);
		if (!authorization.canAccessEmployee(assignment.getEmployeeId())) throw denied();
		return context(assignment);
	}

	@Override @Transactional(readOnly = true)
	public ExecutionAssignment requireOwner(UUID assignmentId, boolean mustBeInProgress) {
		TrainingAssignment assignment = require(assignmentId);
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.role() != UserRole.EMPLOYEE || actor.employeeId() == null
				|| !actor.employeeId().equals(assignment.getEmployeeId())) throw denied();
		if (mustBeInProgress && assignment.getStatus() != AssignmentStatus.IN_PROGRESS)
			throw conflict("ASSIGNMENT_NOT_IN_PROGRESS", "Inicie a atribuição antes de registrar progresso.");
		return context(assignment);
	}

	@Override @Transactional
	public ExecutionAssignment start(UUID assignmentId) {
		TrainingAssignment assignment = requireOwnerEntity(assignmentId);
		if (assignment.getStatus() == AssignmentStatus.IN_PROGRESS) return context(assignment);
		AssignmentStatus previous = assignment.getStatus();
		try { assignment.start(); }
		catch (IllegalStateException exception) { throw conflict("INVALID_STATE_TRANSITION", "A atribuição não pode ser iniciada neste estado."); }
		statusEvents.save(new AssignmentStatusEvent(DEFAULT_ORGANIZATION_ID, assignment.getId(), previous,
				assignment.getStatus(), "LEARNER_STARTED", clock.instant()));
		return context(assignment);
	}

	@Override @Transactional
	public ExecutionAssignment contentReady(UUID assignmentId, boolean hasActiveQuestionnaires) {
		TrainingAssignment assignment = require(assignmentId);
		if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) return context(assignment);
		if (hasActiveQuestionnaires) {
			AssignmentStatus previous = assignment.getStatus();
			assignment.awaitAssessment();
			statusEvents.save(new AssignmentStatusEvent(DEFAULT_ORGANIZATION_ID, assignment.getId(), previous,
					assignment.getStatus(), "CONTENT_READY", clock.instant()));
		} else {
			if (readiness.contentReady(context(assignment))) changeToCompleted(assignment, "CONTENT_READY_NO_ASSESSMENT");
		}
		return context(assignment);
	}

	@Override @Transactional
	public ExecutionAssignment lockForAssessment(UUID assignmentId) {
		TrainingAssignment assignment = assignments.lockByIdAndOrganizationId(assignmentId, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A atribuição informada não existe."));
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.role() != UserRole.EMPLOYEE || actor.employeeId() == null
				|| !actor.employeeId().equals(assignment.getEmployeeId())) throw denied();
		if (assignment.getStatus() != AssignmentStatus.AWAITING_ASSESSMENT
				&& assignment.getStatus() != AssignmentStatus.FAILED)
			throw conflict("ASSESSMENT_NOT_AVAILABLE", "A avaliação não está disponível neste estado.");
		return context(assignment);
	}

	@Override @Transactional
	public ExecutionAssignment assessmentResult(UUID assignmentId, boolean approved, boolean allQuestionnairesPassed) {
		TrainingAssignment assignment = require(assignmentId);
		AssignmentStatus previous = assignment.getStatus();
		try { assignment.assessmentResult(approved, allQuestionnairesPassed); }
		catch (IllegalStateException exception) { throw conflict("INVALID_STATE_TRANSITION", "O resultado não pode ser aplicado neste estado."); }
		if (previous != assignment.getStatus()) statusEvents.save(new AssignmentStatusEvent(DEFAULT_ORGANIZATION_ID,
				assignment.getId(), previous, assignment.getStatus(), approved ? "ASSESSMENT_APPROVED" : "ASSESSMENT_FAILED", clock.instant()));
		return context(assignment);
	}

	@Override @Transactional
	public ExecutionAssignment complete(UUID assignmentId, String reason) {
		TrainingAssignment assignment = require(assignmentId);
		if (assignment.getStatus() == AssignmentStatus.COMPLETED) return context(assignment);
		changeToCompleted(assignment, reason);
		return context(assignment);
	}

	@Override @Transactional(readOnly = true)
	public Optional<ExecutionAssignment> findPlaybackAssignment(UUID employeeId, UUID trainingVersionId) {
		return assignments.findFirstByOrganizationIdAndEmployeeIdAndTrainingVersionIdAndStatusIn(
				DEFAULT_ORGANIZATION_ID, employeeId, trainingVersionId, PLAYABLE).map(this::context);
	}

	private TrainingAssignment requireOwnerEntity(UUID assignmentId) {
		TrainingAssignment assignment = require(assignmentId);
		CurrentUser actor = currentUser.requireCurrentUser();
		if (actor.role() != UserRole.EMPLOYEE || actor.employeeId() == null
				|| !actor.employeeId().equals(assignment.getEmployeeId())) throw denied();
		return assignment;
	}
	private TrainingAssignment require(UUID id) {
		return assignments.findByIdAndOrganizationId(id, DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A atribuição informada não existe."));
	}
	private ExecutionAssignment context(TrainingAssignment item) {
		return new ExecutionAssignment(item.getId(), item.getOrganizationId(), item.getEmployeeId(), item.getTrainingId(),
				item.getTrainingVersionId(), item.getStatus());
	}
	private void changeToCompleted(TrainingAssignment assignment, String reason) {
		AssignmentStatus previous = assignment.getStatus();
		try { assignment.complete(); }
		catch (IllegalStateException exception) { throw conflict("INVALID_STATE_TRANSITION", "A atribuição não pode ser concluída neste estado."); }
		statusEvents.save(new AssignmentStatusEvent(DEFAULT_ORGANIZATION_ID, assignment.getId(), previous,
				assignment.getStatus(), reason, clock.instant()));
	}
	private AccessDeniedException denied() { return new AccessDeniedException("A atribuição pertence a outro colaborador."); }
	private ResourceConflictException conflict(String code, String message) { return new ResourceConflictException(code, message); }
}
