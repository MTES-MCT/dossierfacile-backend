package fr.dossierfacile.api.front.config.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public abstract class AbstractRateLimitingFilter implements Filter {
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    protected abstract int getRegisterCapacity();

    protected abstract int getRefillDelayInMinute();

    protected int getRefillTokensByDelay() {
        return 1;
    }

    protected int getMapSizeLimit() {
        return 10000;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (ipBuckets.size() > getMapSizeLimit()) {
            log.warn("ipBuckets list has been reset");
            ipBuckets.clear();
        }
        // specific to scalingo infra
        Bucket bucket = ipBuckets.computeIfAbsent(((HttpServletRequest) request).getHeader("X-Real-Ip"), this::createNewBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            log.error("Too Many request has been detected from " + ((HttpServletRequest) request).getHeader("X-Real-Ip"));
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(429);
            httpServletResponse.getWriter().write("Too Many Requests");
        }
    }

    private Bucket createNewBucket(String key) {
        return new LocalBucketBuilder().addLimit(Bandwidth.classic(getRegisterCapacity(), Refill.intervally(getRefillTokensByDelay(), Duration.ofMinutes(getRefillDelayInMinute())))).build();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
