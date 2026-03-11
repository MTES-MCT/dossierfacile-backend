package fr.gouv.bo.configuration.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Value;

@WebFilter(urlPatterns = "/documents/*")
public class RateLimitingBoDocumentsFilter extends AbstractDownloadRateLimitingFilter {
    @Value("${ratelimit.bo.documents.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.bo.documents.per.day}")
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
