package dev.igorbarbosa.worktrainingsystem.trainings.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTrainingRequest(
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
		boolean regulatoryStandard) {
}
