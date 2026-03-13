package fr.dossierfacile.api.pdf.config.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * OWASP File Upload — "Only allow authorized users" (remplacé par rate limiting pour API publique).
 * <p>
 * Limite le nombre d'uploads par IP pour éviter les abus sur l'endpoint public.
 */
@Configuration
public class UploadRateLimitingFilter extends AbstractDownloadRateLimitingFilter {

    @Value("${ratelimit.upload.per.minute:10}")
    private int perMinuteCapacity;

    @Value("${ratelimit.upload.per.day:200}")
    private int perDayCapacity;

    @Override
    protected int getPerMinuteCapacity() {
        return perMinuteCapacity;
    }

    @Override
    protected int getPerDayCapacity() {
        return perDayCapacity;
    }

    @Bean
    public FilterRegistrationBean<UploadRateLimitingFilter> uploadRateLimitFilter() {
        FilterRegistrationBean<UploadRateLimitingFilter> registration = new FilterRegistrationBean<>(this);
        registration.addUrlPatterns("/api/document/files");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
