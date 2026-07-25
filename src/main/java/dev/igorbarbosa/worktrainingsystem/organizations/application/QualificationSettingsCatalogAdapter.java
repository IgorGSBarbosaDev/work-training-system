package dev.igorbarbosa.worktrainingsystem.organizations.application;

import dev.igorbarbosa.worktrainingsystem.organizations.persistence.OrganizationSettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class QualificationSettingsCatalogAdapter implements QualificationSettingsCatalog {
	private final OrganizationSettingsRepository settings;
	QualificationSettingsCatalogAdapter(OrganizationSettingsRepository settings) { this.settings = settings; }
	@Override @Transactional(readOnly = true)
	public int expiringSoonDays(UUID organizationId) {
		return settings.findById(organizationId).map(value -> value.getExpiringSoonDays()).orElse(30);
	}
}
