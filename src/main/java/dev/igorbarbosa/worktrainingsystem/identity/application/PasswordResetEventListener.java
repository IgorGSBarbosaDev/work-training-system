package dev.igorbarbosa.worktrainingsystem.identity.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class PasswordResetEventListener {
	private final PasswordResetDeliveryPort deliveryPort;
	PasswordResetEventListener(PasswordResetDeliveryPort deliveryPort) { this.deliveryPort = deliveryPort; }

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void deliver(PasswordResetRequested event) {
		deliveryPort.deliver(event.email(), event.opaqueToken());
	}
}
