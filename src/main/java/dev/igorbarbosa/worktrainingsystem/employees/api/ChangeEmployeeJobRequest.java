package dev.igorbarbosa.worktrainingsystem.employees.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeEmployeeJobRequest(
		@NotNull(message = "O cargo é obrigatório.") UUID jobId,
		@NotNull(message = "A opção de remoção das atividades anteriores é obrigatória.")
		Boolean removePreviousJobActivities) {
}
