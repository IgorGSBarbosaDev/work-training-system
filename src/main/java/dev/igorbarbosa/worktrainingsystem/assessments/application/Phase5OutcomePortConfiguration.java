package dev.igorbarbosa.worktrainingsystem.assessments.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class Phase5OutcomePortConfiguration {
	@Bean @ConditionalOnMissingBean(CertificateIssuancePort.class)
	CertificateIssuancePort pendingCertificatePort() { return completionId -> { }; }
	@Bean @ConditionalOnMissingBean(TrainingNotificationPort.class)
	TrainingNotificationPort pendingNotificationPort() { return event -> { }; }
}
