package dev.igorbarbosa.worktrainingsystem.employees.application;

import dev.igorbarbosa.worktrainingsystem.shared.storage.application.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class EmployeePhotoCleanupListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmployeePhotoCleanupListener.class);
	private final ObjectStorage storage;

	EmployeePhotoCleanupListener(ObjectStorage storage) { this.storage = storage; }

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void deleteOldPhoto(EmployeePhotoReplaced event) {
		if (event.oldObjectKey() == null) return;
		try { storage.delete(event.oldObjectKey()); }
		catch (RuntimeException exception) {
			LOGGER.error("Failed to delete replaced employee photo object", exception);
		}
	}
}
