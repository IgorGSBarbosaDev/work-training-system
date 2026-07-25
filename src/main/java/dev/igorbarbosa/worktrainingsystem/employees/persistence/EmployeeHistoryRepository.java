package dev.igorbarbosa.worktrainingsystem.employees.persistence;

import dev.igorbarbosa.worktrainingsystem.employees.domain.EmployeeHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, UUID> {
	Page<EmployeeHistory> findAllByEmployeeId(UUID employeeId, Pageable pageable);
}
