package fr.dossierfacile.common.config.filter;

import fr.dossierfacile.logging.util.LoggerUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractDownloadRateLimitingFilter implements Filter {
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    protected abstract int getPerMinuteCapacity();

    protected abstract int getPerDayCapacity();

    protected int getMapSizeLimit() {
        return 10000;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (ipBuckets.size() > getMapSizeLimit()) {
            log.warn("ipBuckets list has been reset");
            ipBuckets.clear();
        }
        String ip = LoggerUtil.getRealIp((HttpServletRequest) request);
        Bucket bucket = ipBuckets.computeIfAbsent(ip, this::createNewBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            log.error("Too Many request has been detected from IP address: {}", ip);
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(429);
            httpServletResponse.getWriter().write("Too Many Requests");
        }
    }

    private Bucket createNewBucket(String key) {
        return new LocalBucketBuilder()
                .addLimit(Bandwidth.classic(getPerMinuteCapacity(),
                        Refill.intervally(getPerMinuteCapacity(), Duration.ofMinutes(1))))
                .addLimit(Bandwidth.classic(getPerDayCapacity(),
                        Refill.intervally(getPerDayCapacity(), Duration.ofDays(1))))
                .build();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
