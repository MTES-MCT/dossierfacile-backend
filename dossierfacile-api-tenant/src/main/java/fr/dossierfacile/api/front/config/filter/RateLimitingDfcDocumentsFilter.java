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

@WebFilter(urlPatterns = {"/dfc/api/v1/tenants/*"})
public class RateLimitingDfcDocumentsFilter extends AbstractDownloadRateLimitingFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String DOCUMENTS_PATTERN = "/dfc/api/v1/tenants/*/documents/**";

    @Value("${ratelimit.dfc.documents.per.minute}")
    private int perMinuteCapacity;
    @Value("${ratelimit.dfc.documents.per.day}")
    private int perDayCapacity;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // Only rate-limit document downloads, not the tenant list/detail JSON endpoints
        if (!PATH_MATCHER.match(DOCUMENTS_PATTERN, httpRequest.getRequestURI())) {
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
