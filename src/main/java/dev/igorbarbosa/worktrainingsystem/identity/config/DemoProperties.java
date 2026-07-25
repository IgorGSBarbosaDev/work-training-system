package dev.igorbarbosa.worktrainingsystem.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.demo")
public record DemoProperties(boolean enabled, String adminEmail, String adminPassword) {
}
