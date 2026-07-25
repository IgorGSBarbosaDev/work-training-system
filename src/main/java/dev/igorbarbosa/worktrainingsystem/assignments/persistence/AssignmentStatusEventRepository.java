package dev.igorbarbosa.worktrainingsystem.assignments.persistence;

import dev.igorbarbosa.worktrainingsystem.assignments.domain.AssignmentStatusEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentStatusEventRepository extends JpaRepository<AssignmentStatusEvent, UUID> {
}
