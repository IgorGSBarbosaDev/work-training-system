package dev.igorbarbosa.worktrainingsystem.notifications.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SliceBNotificationConfiguration {
	@Bean @ConditionalOnMissingBean(SliceBNotificationPort.class)
	SliceBNotificationPort pendingSliceBNotifications() {
		return new SliceBNotificationPort() {
			@Override public void expirationChanged(ExpirationNotification event) {}
			@Override public void qualificationBlocked(QualificationBlockedNotification event) {}
		};
	}
}
