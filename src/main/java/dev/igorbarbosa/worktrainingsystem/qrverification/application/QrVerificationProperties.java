package dev.igorbarbosa.worktrainingsystem.qrverification.application;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.qr")
public record QrVerificationProperties(@NotNull URI publicBaseUrl) {

	public URI verificationUrl(String token) {
		String base = publicBaseUrl.toString();
		while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
		return URI.create(base + "/verificar/" + token);
	}
}
