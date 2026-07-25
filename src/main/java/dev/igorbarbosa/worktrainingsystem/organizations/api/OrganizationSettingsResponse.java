package dev.igorbarbosa.worktrainingsystem.organizations.api;

import dev.igorbarbosa.worktrainingsystem.organizations.domain.OrganizationSettings;

public record OrganizationSettingsResponse(int expiringSoonDays, int defaultPassingScore,
		int defaultRequiredVideoPercentage) {
	public static OrganizationSettingsResponse from(OrganizationSettings settings) {
		return new OrganizationSettingsResponse(settings.getExpiringSoonDays(), settings.getDefaultPassingScore(),
				settings.getDefaultRequiredVideoPercentage());
	}
}
