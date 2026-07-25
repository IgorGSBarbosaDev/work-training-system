package dev.igorbarbosa.worktrainingsystem.organizations.application;

import static dev.igorbarbosa.worktrainingsystem.shared.persistence.OrganizationScope.DEFAULT_ORGANIZATION_ID;

import dev.igorbarbosa.worktrainingsystem.organizations.api.OrganizationResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.OrganizationSettingsResponse;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateOrganizationRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.api.UpdateOrganizationSettingsRequest;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.Organization;
import dev.igorbarbosa.worktrainingsystem.organizations.domain.OrganizationSettings;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.OrganizationRepository;
import dev.igorbarbosa.worktrainingsystem.organizations.persistence.OrganizationSettingsRepository;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.BusinessRuleViolationException;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
	private final OrganizationRepository organizations;
	private final OrganizationSettingsRepository settings;

	public OrganizationService(OrganizationRepository organizations, OrganizationSettingsRepository settings) {
		this.organizations = organizations;
		this.settings = settings;
	}

	@Transactional(readOnly = true)
	public OrganizationResponse get() { return OrganizationResponse.from(findOrganization()); }

	@Transactional
	public OrganizationResponse update(UpdateOrganizationRequest request) {
		if (!request.hasChanges()) throw noChanges();
		Organization organization = findOrganization();
		organization.update(request.name() == null ? organization.getName() : request.name().trim(),
				request.status() == null ? organization.getStatus() : request.status());
		return OrganizationResponse.from(organization);
	}

	@Transactional(readOnly = true)
	public OrganizationSettingsResponse getSettings() {
		return OrganizationSettingsResponse.from(findSettings());
	}

	@Transactional
	public OrganizationSettingsResponse updateSettings(UpdateOrganizationSettingsRequest request) {
		if (!request.hasChanges()) throw noChanges();
		OrganizationSettings current = findSettings();
		int videoPercentage = request.defaultRequiredVideoPercentage() == null
				? current.getDefaultRequiredVideoPercentage() : request.defaultRequiredVideoPercentage();
		if (videoPercentage != 80) {
			throw new BusinessRuleViolationException("VIDEO_THRESHOLD_FIXED",
					"O percentual obrigatório de vídeo do MVP deve permanecer em 80.");
		}
		current.update(request.expiringSoonDays() == null ? current.getExpiringSoonDays() : request.expiringSoonDays(),
				request.defaultPassingScore() == null ? current.getDefaultPassingScore() : request.defaultPassingScore());
		return OrganizationSettingsResponse.from(current);
	}

	private Organization findOrganization() {
		return organizations.findById(DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("A organização atual não existe."));
	}

	private OrganizationSettings findSettings() {
		return settings.findById(DEFAULT_ORGANIZATION_ID)
				.orElseThrow(() -> new ResourceNotFoundException("As configurações da organização não existem."));
	}

	private BusinessRuleViolationException noChanges() {
		return new BusinessRuleViolationException("NO_CHANGES", "Informe ao menos um campo para atualização.");
	}
}
