package fr.dossierfacile.api.front.config.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Value;

@WebFilter(urlPatterns = {"/api/application/zip", "/api/file/resource/*", "/api/document/resource/*", "/api/file/preview/*"})
public class RateLimitingAuthenticatedDownloadFilter extends AbstractDownloadRateLimitingFilter {
    @Value("${ratelimit.authenticated.download.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.authenticated.download.per.day}")
    private int perDayCapacity;

    @Override
    protected int getPerMinuteCapacity() {
        return perMinuteCapacity;
    }

    @Override
    protected int getPerDayCapacity() {
        return perDayCapacity;
    }
}
