package dev.igorbarbosa.worktrainingsystem.organizations.application;

import java.util.UUID;

public interface QualificationSettingsCatalog {
	int expiringSoonDays(UUID organizationId);
}
