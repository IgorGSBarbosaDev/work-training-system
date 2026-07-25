package dev.igorbarbosa.worktrainingsystem.expirations.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ExpirationScheduler {
	private final ExpirationService service;
	ExpirationScheduler(ExpirationService service) { this.service = service; }
	@Scheduled(cron = "${app.expirations.cron:0 15 1 * * *}", zone = "UTC")
	void processUtc() { service.recalculate(); }
}
