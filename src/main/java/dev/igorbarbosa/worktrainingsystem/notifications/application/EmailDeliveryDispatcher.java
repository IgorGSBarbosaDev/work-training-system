package dev.igorbarbosa.worktrainingsystem.notifications.application;

import dev.igorbarbosa.worktrainingsystem.notifications.domain.EmailDelivery;
import dev.igorbarbosa.worktrainingsystem.notifications.persistence.EmailDeliveryRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailDeliveryDispatcher {
	private final EmailDeliveryRepository deliveries;
	private final JavaMailSender mailSender;
	private final Clock clock;

	public EmailDeliveryDispatcher(EmailDeliveryRepository deliveries, JavaMailSender mailSender, Clock clock) {
		this.deliveries = deliveries;
		this.mailSender = mailSender;
		this.clock = clock;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void dispatch(EmailDeliveryQueued event) {
		deliveries.findByIdAndOrganizationId(event.deliveryId(), event.organizationId())
				.ifPresent(this::send);
	}

	private void send(EmailDelivery delivery) {
		try {
			var mail = new SimpleMailMessage();
			mail.setTo(delivery.getRecipient());
			mail.setSubject(delivery.getSubject());
			mail.setText(delivery.getBody());
			mailSender.send(mail);
			delivery.sent(clock.instant());
		} catch (RuntimeException exception) {
			delivery.failed(safeError(exception), clock.instant());
		}
	}

	private String safeError(RuntimeException exception) {
		return "Falha SMTP: " + exception.getClass().getSimpleName();
	}

	public record EmailDeliveryQueued(UUID organizationId, UUID deliveryId) {}
}
