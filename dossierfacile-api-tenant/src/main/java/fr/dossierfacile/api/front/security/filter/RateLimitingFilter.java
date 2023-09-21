package fr.dossierfacile.api.front.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@WebFilter("/api/register/account")
public class RateLimitingFilter implements Filter {
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    @Value("${ratelimit.register.capacity}")
    private int registerCapacity;
    @Value("${ratelimit.register.refill.delay.in.minute}")
    private int refillDelayInMinute;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Bucket bucket = ipBuckets.computeIfAbsent(request.getRemoteAddr(), this::createNewBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(429);
            httpServletResponse.getWriter().write("Too Many Requests");
        }
    }

    private Bucket createNewBucket(String key) {
        return new LocalBucketBuilder().addLimit(Bandwidth.classic(registerCapacity, Refill.intervally(1, Duration.ofMinutes(refillDelayInMinute)))).build();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
