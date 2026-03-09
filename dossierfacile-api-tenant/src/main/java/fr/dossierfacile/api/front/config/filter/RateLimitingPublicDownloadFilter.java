package fr.dossierfacile.api.front.config.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Value;

@WebFilter(urlPatterns = {"/api/application/fullPdf/*",  "api/application/light/*", "api/application/full/*", "/api/application/links/*/documents/*"})
public class RateLimitingPublicDownloadFilter extends AbstractDownloadRateLimitingFilter {
    @Value("${ratelimit.public.download.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.public.download.per.day}")
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
