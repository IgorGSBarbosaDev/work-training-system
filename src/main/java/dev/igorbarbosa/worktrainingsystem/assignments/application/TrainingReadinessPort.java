package dev.igorbarbosa.worktrainingsystem.assignments.application;

/** Slice B replaces this adapter to create completion, validity and qualification evidence. */
public interface TrainingReadinessPort {
	boolean contentReady(AssignmentExecutionPort.ExecutionAssignment assignment);
}
