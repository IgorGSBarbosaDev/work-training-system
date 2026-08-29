package dev.igorbarbosa.worktrainingsystem.reporting.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enables caching only when the complete application configuration is loaded. */
@Configuration
@EnableCaching
public class ReportingCacheConfiguration {
}
