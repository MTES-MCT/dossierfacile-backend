package fr.dossierfacile.common.config.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisRateLimiterService redisRateLimiterService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RateLimit rateLimit;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(redisRateLimiterService);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void checkRateLimit_allowsProceed_whenRateLimitNotExceeded() throws Throwable {
        Method sampleMethod = DummyClass.class.getMethod("dummyMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(rateLimit.name()).thenReturn("dummy-bucket");
        when(rateLimit.perMinute()).thenReturn(10);
        when(rateLimit.perDay()).thenReturn(0);
        when(redisRateLimiterService.tryConsume(eq("dummy-bucket:127.0.0.1"), eq(10), any(Duration.class))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.checkRateLimit(joinPoint, rateLimit);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void checkRateLimit_throwsException_whenRateLimitExceeded() throws Throwable {
        Method sampleMethod = DummyClass.class.getMethod("dummyMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(sampleMethod);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(rateLimit.name()).thenReturn("dummy-bucket");
        when(rateLimit.perMinute()).thenReturn(10);
        when(rateLimit.perDay()).thenReturn(0);
        when(redisRateLimiterService.tryConsume(eq("dummy-bucket:127.0.0.1"), eq(10), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> aspect.checkRateLimit(joinPoint, rateLimit))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too Many Requests");
    }

    // Only use for test purpose
    static class DummyClass {
        public void dummyMethod() {
            // Empty for test purpose
        }
    }
}
