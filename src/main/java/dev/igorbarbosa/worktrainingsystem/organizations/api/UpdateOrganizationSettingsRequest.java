package dev.igorbarbosa.worktrainingsystem.organizations.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateOrganizationSettingsRequest(
		@Min(value = 1, message = "A janela deve possuir ao menos um dia.")
		@Max(value = 3650, message = "A janela deve possuir no máximo 3650 dias.") Integer expiringSoonDays,
		@Min(value = 70, message = "A nota mínima padrão não pode ser inferior a 70.")
		@Max(value = 100, message = "A nota mínima padrão não pode ser superior a 100.") Integer defaultPassingScore,
		@Min(value = 80, message = "O percentual obrigatório de vídeo do MVP é 80.")
		@Max(value = 80, message = "O percentual obrigatório de vídeo do MVP é 80.") Integer defaultRequiredVideoPercentage) {
	public boolean hasChanges() {
		return expiringSoonDays != null || defaultPassingScore != null || defaultRequiredVideoPercentage != null;
	}
}
