package dev.igorbarbosa.worktrainingsystem.identity.config;

import dev.igorbarbosa.worktrainingsystem.identity.application.AdministrativeAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(AdministrativeAuditInterceptor.class)
public class AuditWebConfiguration implements WebMvcConfigurer {
	private final AdministrativeAuditInterceptor interceptor;
	public AuditWebConfiguration(AdministrativeAuditInterceptor interceptor) { this.interceptor = interceptor; }
	@Override public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**");
	}
}
