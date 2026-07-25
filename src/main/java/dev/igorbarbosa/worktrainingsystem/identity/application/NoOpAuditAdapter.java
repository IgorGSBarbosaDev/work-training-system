package dev.igorbarbosa.worktrainingsystem.identity.application;

import org.springframework.stereotype.Component;

@Component
class NoOpAuditAdapter implements AuditPort {
	@Override
	public void record(AuditRecord record) {
		// Phase 5 can replace this adapter with durable audit persistence.
	}
}
