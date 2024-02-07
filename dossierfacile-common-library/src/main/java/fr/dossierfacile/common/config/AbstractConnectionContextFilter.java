package fr.dossierfacile.common.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class AbstractConnectionContextFilter extends HttpFilter {

    private static final String URI = "uri";
    private static final String REQUEST_ID = "request_id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (isRequestIgnored(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            MDC.put(URI, httpRequest.getRequestURI());
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());

            getAdditionalContextElements().forEach(MDC::put);

            log.info("Request: method={}, path={}", httpRequest.getMethod(), httpRequest.getRequestURI());
        } catch (Exception e) {
            // Something wrong but service should stay up
            log.warn("Unable to inject data in MDC", e);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            log.info("Response: status={}, method={}, path={}",
                    ((HttpServletResponse) response).getStatus(),
                    httpRequest.getMethod(), httpRequest.getRequestURI());
            MDC.clear();
        }
    }

    public abstract boolean isRequestIgnored(HttpServletRequest request);

    public abstract Map<String, String> getAdditionalContextElements();

}