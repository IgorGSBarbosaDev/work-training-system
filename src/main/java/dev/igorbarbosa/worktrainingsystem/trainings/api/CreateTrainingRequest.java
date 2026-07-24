package dev.igorbarbosa.worktrainingsystem.trainings.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.igorbarbosa.worktrainingsystem.shared.domain.RegistrationStatus;
import dev.igorbarbosa.worktrainingsystem.trainings.domain.ValidityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTrainingRequest(
		@NotBlank(message = "O nome é obrigatório.")
		@Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
		String name,
		@NotBlank(message = "O código é obrigatório.")
		@Size(max = 50, message = "O código deve possuir no máximo 50 caracteres.")
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "O código possui formato inválido.")
		String code,
		@Size(max = 2000, message = "A descrição deve possuir no máximo 2000 caracteres.")
		String description,
		@Size(max = 150, message = "A categoria deve possuir no máximo 150 caracteres.")
		String category,
		@JsonProperty("isRegulatoryStandard")
		boolean regulatoryStandard,
		@NotNull(message = "O status é obrigatório.")
		RegistrationStatus status,
		@NotNull(message = "A versão inicial é obrigatória.")
		@Valid
		InitialVersion initialVersion) {

	public record InitialVersion(
			@Positive(message = "A carga horária deve ser positiva.")
			int workloadMinutes,
			@NotNull(message = "O tipo de validade é obrigatório.")
			ValidityType validityType,
			@Positive(message = "O valor da validade deve ser positivo.")
			Integer validityValue,
			@NotNull(message = "A nota mínima é obrigatória.")
			@DecimalMin(value = "0.0")
			@DecimalMax(value = "100.0")
			BigDecimal passingScore,
			@Positive(message = "O máximo de tentativas deve ser positivo.")
			Integer maxAttempts,
			@Min(value = 0, message = "O intervalo entre tentativas não pode ser negativo.")
			int retryIntervalMinutes) {
	}
}
