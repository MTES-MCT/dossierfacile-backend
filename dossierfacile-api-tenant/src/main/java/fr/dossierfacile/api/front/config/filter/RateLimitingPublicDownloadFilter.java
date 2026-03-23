package fr.dossierfacile.api.front.config.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

@WebFilter(urlPatterns = {"/api/application/fullPdf/*", "/api/application/light/*", "/api/application/full/*", "/api/application/links/*"})
public class RateLimitingPublicDownloadFilter extends AbstractDownloadRateLimitingFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String LINKS_DOCUMENTS_PATTERN = "/api/application/links/*/documents/**";

    @Value("${ratelimit.public.download.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.public.download.per.day}")
    private int perDayCapacity;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        // Only rate-limit document downloads under /links/, not link management endpoints
        if (path.startsWith("/api/application/links/")
                && !PATH_MATCHER.match(LINKS_DOCUMENTS_PATTERN, path)) {
            chain.doFilter(request, response);
            return;
        }
        super.doFilter(request, response, chain);
    }

    @Override
    protected int getPerMinuteCapacity() {
        return perMinuteCapacity;
    }

    @Override
    protected int getPerDayCapacity() {
        return perDayCapacity;
    }
}
