package fr.dossierfacile.common.config.ratelimit;

import fr.dossierfacile.logging.util.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect implements EmbeddedValueResolverAware {

    private final RedisRateLimiterService redisRateLimiterService;
    private StringValueResolver valueResolver;

    @Override
    public void setEmbeddedValueResolver(@NonNull StringValueResolver resolver) {
        this.valueResolver = resolver;
    }

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = LoggerUtil.getRealIp(request);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String bucketName = rateLimit.name().isEmpty()
                ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
                : rateLimit.name();
        String rateLimitKey = bucketName + ":" + ip;

        int perMinute = resolveIntValue(rateLimit.perMinute(), rateLimit.perMinuteString());
        int perDay = resolveIntValue(rateLimit.perDay(), rateLimit.perDayString());

        boolean allowed = true;

        if (perMinute > 0 || perDay > 0) {
            if (perMinute > 0) {
                allowed = redisRateLimiterService.tryConsume(rateLimitKey, perMinute, Duration.ofMinutes(1));
            }
            if (perDay > 0) {
                allowed = allowed && redisRateLimiterService.tryConsume(rateLimitKey, perDay, Duration.ofDays(1));
            }
        } else {
            Duration duration = Duration.of(rateLimit.period(), rateLimit.unit().toChronoUnit());
            allowed = redisRateLimiterService.tryConsume(rateLimitKey, rateLimit.capacity(), duration);
        }

        if (!allowed) {
            log.error("Too Many requests detected from IP address: {} on bucket {}", ip, bucketName);
            throw new RateLimitExceededException("Too Many Requests");
        }

        return joinPoint.proceed();
    }

    private int resolveIntValue(int defaultValue, String propertyExpression) {
        if (propertyExpression != null && !propertyExpression.isBlank() && valueResolver != null) {
            try {
                String resolvedStr = valueResolver.resolveStringValue(propertyExpression);
                if (resolvedStr != null && !resolvedStr.isBlank()) {
                    return Integer.parseInt(resolvedStr.trim());
                }
            } catch (Exception e) {
                log.warn("Could not resolve property expression {} for rate limiting", propertyExpression, e);
            }
        }
        return defaultValue;
    }
}
