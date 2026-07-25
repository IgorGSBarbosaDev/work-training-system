package dev.igorbarbosa.worktrainingsystem.identity.application;

class NoOpAuditAdapter implements AuditPort {
	@Override
	public void record(AuditRecord record) {
		// Phase 5 can replace this adapter with durable audit persistence.
	}
}
