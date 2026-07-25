package dev.igorbarbosa.worktrainingsystem.activities.application;

public interface AssignmentGenerationPort {
	int generate(ActivityAssignmentRequested event);
}
