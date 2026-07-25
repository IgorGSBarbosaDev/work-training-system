package dev.igorbarbosa.worktrainingsystem.organizations.persistence;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.OrganizationSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {}
