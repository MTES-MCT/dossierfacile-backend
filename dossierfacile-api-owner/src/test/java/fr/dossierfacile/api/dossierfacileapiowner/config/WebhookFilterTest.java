package fr.dossierfacile.api.dossierfacileapiowner.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookFilterTest {

    private static final String HEADER_NAME = "X-Api-Key";
    private static final String SECRET_TOKEN = "secret-api-key-123";

    @Mock
    private FilterChain filterChain;

    private WebhookFilter webhookFilter;

    @BeforeEach
    void setUp() {
        webhookFilter = new WebhookFilter(SECRET_TOKEN, HEADER_NAME);
    }

    @Test
    void doFilter_whenApiKeyHeaderIsMissing_shouldRejectRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhook/");
        request.setServletPath("/webhook/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        webhookFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_whenApiKeyIsInvalid_shouldRejectRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhook/");
        request.setServletPath("/webhook/");
        request.addHeader(HEADER_NAME, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        webhookFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_whenApiKeyIsValid_shouldAllowRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhook/");
        request.setServletPath("/webhook/");
        request.addHeader(HEADER_NAME, SECRET_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        webhookFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain).doFilter(request, response);
    }
}
