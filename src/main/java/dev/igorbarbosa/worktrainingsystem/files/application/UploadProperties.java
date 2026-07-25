package dev.igorbarbosa.worktrainingsystem.files.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.uploads")
public record UploadProperties(boolean allowLegacyObjectKeys) {
}
