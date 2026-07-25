package dev.igorbarbosa.worktrainingsystem.activities.application;

import java.util.Set;
import java.util.UUID;

public record QualificationRecalculationRequested(UUID organizationId, Set<UUID> employeeIds, UUID activityId) {}
