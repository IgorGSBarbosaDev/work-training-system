package dev.igorbarbosa.worktrainingsystem.organizations.persistence;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.Organization;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {}
