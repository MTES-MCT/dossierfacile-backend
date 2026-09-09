package fr.dossierfacile.common.config.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RedisRateLimiterService(redisTemplate);
    }

    @Test
    void tryConsume_allowsRequest_whenUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = service.tryConsume("test-bucket", 5, Duration.ofMinutes(1));

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void tryConsume_blocksRequest_whenExceedingLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(6L);

        boolean allowed = service.tryConsume("test-bucket", 5, Duration.ofMinutes(1));

        assertThat(allowed).isFalse();
    }

    @Test
    void tryConsume_failsOpen_onRedisException() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        boolean allowed = service.tryConsume("test-bucket", 5, Duration.ofMinutes(1));

        assertThat(allowed).isTrue();
    }
}
