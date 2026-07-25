package dev.igorbarbosa.worktrainingsystem.identity.application;

import dev.igorbarbosa.worktrainingsystem.identity.config.IdentityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class MailPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailPasswordResetDeliveryAdapter.class);
	private final JavaMailSender mailSender;
	private final IdentityProperties properties;
	private final String from;

	MailPasswordResetDeliveryAdapter(JavaMailSender mailSender, IdentityProperties properties,
			@Value("${app.mail.from}") String from) {
		this.mailSender = mailSender; this.properties = properties; this.from = from;
	}

	@Override
	public void deliver(String email, String opaqueToken) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email);
		message.setSubject("Redefinição de senha");
		message.setText("Use o link para redefinir sua senha: " + properties.passwordResetUrl() + "?token=" + opaqueToken);
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			LOGGER.warn("Password reset email delivery failed; the token remains valid until expiry");
		}
	}
}
