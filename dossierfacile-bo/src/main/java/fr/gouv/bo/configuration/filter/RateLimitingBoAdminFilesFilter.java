package fr.gouv.bo.configuration.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Value;

@WebFilter(urlPatterns = "/files/*")
public class RateLimitingBoAdminFilesFilter extends AbstractDownloadRateLimitingFilter {
    @Value("${ratelimit.bo.admin.files.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.bo.admin.files.per.day}")
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
