package fr.gouv.bo.configuration.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitingBoAdminFilesFilterTest {

    private FilterChain mockChain;

    @BeforeEach
    void setUp() {
        mockChain = mock(FilterChain.class);
    }

    private RateLimitingBoAdminFilesFilter filterWith(int perMinute, int perDay) {
        RateLimitingBoAdminFilesFilter filter = new RateLimitingBoAdminFilesFilter();
        ReflectionTestUtils.setField(filter, "perMinuteCapacity", perMinute);
        ReflectionTestUtils.setField(filter, "perDayCapacity", perDay);
        return filter;
    }

    private MockHttpServletRequest requestFromIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", ip);
        return request;
    }

    @Test
    void requestsWithinPerMinuteLimitPassThrough() throws Exception {
        RateLimitingBoAdminFilesFilter filter = filterWith(2, 100);
        MockHttpServletRequest request = requestFromIp("10.0.0.1");

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, mockChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(mockChain, times(2)).doFilter(any(), any());
    }

    @Test
    void requestExceedingPerMinuteLimitReturns429() throws Exception {
        RateLimitingBoAdminFilesFilter filter = filterWith(2, 100);
        MockHttpServletRequest request = requestFromIp("10.0.0.1");

        filter.doFilter(request, new MockHttpServletResponse(), mockChain);
        filter.doFilter(request, new MockHttpServletResponse(), mockChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, mockChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        verify(mockChain, times(2)).doFilter(any(), any());
    }

    @Test
    void requestExceedingPerDayLimitReturns429() throws Exception {
        // per-minute higher than per-day so day limit is hit first
        RateLimitingBoAdminFilesFilter filter = filterWith(100, 3);
        MockHttpServletRequest request = requestFromIp("10.0.0.2");

        for (int i = 0; i < 3; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), mockChain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, mockChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        verify(mockChain, times(3)).doFilter(any(), any());
    }

    @Test
    void differentIpsHaveIndependentBuckets() throws Exception {
        RateLimitingBoAdminFilesFilter filter = filterWith(1, 100);

        MockHttpServletRequest requestIp1 = requestFromIp("10.1.1.1");
        MockHttpServletRequest requestIp2 = requestFromIp("10.1.1.2");

        // Exhaust limit for IP1
        filter.doFilter(requestIp1, new MockHttpServletResponse(), mockChain);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(requestIp1, blockedResponse, mockChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(429);

        // IP2 should still be allowed
        MockHttpServletResponse ip2Response = new MockHttpServletResponse();
        filter.doFilter(requestIp2, ip2Response, mockChain);
        assertThat(ip2Response.getStatus()).isEqualTo(200);
    }

    @Test
    void requestWithoutForwardedForHeaderUsesRemoteAddr() throws Exception {
        RateLimitingBoAdminFilesFilter filter = filterWith(1, 100);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mockChain);
        assertThat(response.getStatus()).isEqualTo(200);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, mockChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }
}
