package dev.igorbarbosa.worktrainingsystem.identity.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.igorbarbosa.worktrainingsystem.identity.application.AuthenticationService;
import dev.igorbarbosa.worktrainingsystem.identity.application.UserAdministrationService;
import dev.igorbarbosa.worktrainingsystem.identity.web.AuthController;
import dev.igorbarbosa.worktrainingsystem.identity.web.UserController;
import dev.igorbarbosa.worktrainingsystem.shared.config.JwtProperties;
import dev.igorbarbosa.worktrainingsystem.shared.web.error.RequestCorrelationFilter;
import dev.igorbarbosa.worktrainingsystem.shared.web.pagination.PaginationFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfiguration.class, SecurityErrorWriter.class, RequestCorrelationFilter.class})
@EnableConfigurationProperties({JwtProperties.class, IdentityProperties.class, CorsProperties.class})
@TestPropertySource(properties = {
		"app.jwt.issuer=test-issuer",
		"app.jwt.audience=test-api",
		"app.jwt.signing-secret=01234567890123456789012345678901",
		"app.jwt.access-token-ttl=15m",
		"app.jwt.refresh-token-ttl=7d",
		"app.identity.login-failure-threshold=5",
		"app.identity.login-lock-duration=15m",
		"app.identity.login-attempt-window=15m",
		"app.identity.password-reset-token-ttl=30m",
		"app.identity.password-reset-url=http://localhost/reset-password",
		"app.identity.bcrypt-strength=12",
		"app.cors.allowed-origins=http://localhost:5173"
})
class IdentityEndpointSecurityTest {
	@Autowired MockMvc mvc;
	@MockitoBean AuthenticationService authenticationService;
	@MockitoBean UserAdministrationService userAdministrationService;
	@MockitoBean PaginationFactory paginationFactory;

	@Test
	void protectedEndpointReturnsStandardUnauthorizedShapeAndRequestId() throws Exception {
		mvc.perform(get("/api/v1/auth/me").header("X-Request-Id", "security-test"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("X-Request-Id", "security-test"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
				.andExpect(jsonPath("$.requestId").value("security-test"));
	}

	@Test
	void employeeJwtCannotUseAdminUserEndpointAndGetsStandardForbiddenShape() throws Exception {
		mvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
					.with(jwt().jwt(token -> token.claim("role", "EMPLOYEE"))
							.authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
					.header("X-Request-Id", "forbidden-test"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
				.andExpect(jsonPath("$.requestId").value("forbidden-test"));
	}

	@Test
	void forgotPasswordIsPublicAndDoesNotRevealWhetherEmailExists() throws Exception {
		mvc.perform(post("/api/v1/auth/password/forgot")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"unknown@example.com\"}"))
				.andExpect(status().isAccepted());
	}
}
