package dev.igorbarbosa.worktrainingsystem.shared.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

	private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void keepsSafeRequestIdInResponseAndMdc() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> requestIdDuringRequest = new AtomicReference<>();
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-request_123:abc");

		filter.doFilter(request, response, (currentRequest, currentResponse) ->
				requestIdDuringRequest.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)));

		assertThat(requestIdDuringRequest).hasValue("client-request_123:abc");
		assertThat(request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE))
				.isEqualTo("client-request_123:abc");
		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
				.isEqualTo("client-request_123:abc");
		assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	@Test
	void replacesUnsafeRequestIdInsteadOfReflectingIt() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe\r\nheader");

		filter.doFilter(request, response, (currentRequest, currentResponse) -> { });

		String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
		assertThat(requestId).isNotEqualTo("unsafe\r\nheader");
		assertThatCodeIsUuid(requestId);
	}

	private void assertThatCodeIsUuid(String requestId) {
		assertThat(UUID.fromString(requestId)).isNotNull();
	}
}
