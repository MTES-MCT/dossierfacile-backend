package fr.dossierfacile.common.config.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class RedisRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String keyPrefix, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }

        if (redisTemplate == null) {
            log.trace("StringRedisTemplate is not configured. Rate limiting bypassed for key {}", keyPrefix);
            return true;
        }

        try {
            long windowSeconds = window.getSeconds();
            long currentWindow = Instant.now().getEpochSecond() / Math.max(1, windowSeconds);
            String redisKey = "ratelimit:" + keyPrefix + ":" + windowSeconds + ":" + currentWindow;

            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds * 2));
            }

            return count != null && count <= limit;
        } catch (Exception e) {
            log.error("Error communicating with Redis for rate limiting on key prefix {}. Failing open.", keyPrefix, e);
            return true;
        }
    }
}
