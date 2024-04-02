package fr.dossierfacile.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class AbstractConnectionContextFilter extends HttpFilter {

    private static final String URI = "uri";
    private static final String REQUEST_ID = "request_id";
    private static final String RESPONSE_STATUS = "response_status";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isRequestIgnored(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            MDC.put(URI, request.getRequestURI());
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());

            getAdditionalContextElements().forEach(MDC::put);

            log.info("Request: method={}, path={}", request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            // Something wrong but service should stay up
            log.warn("Unable to inject data in MDC", e);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (response.getStatus() != 200) {
                MDC.put(RESPONSE_STATUS, String.valueOf(response.getStatus()));
                if (response.getStatus() < 400) {
                    log.info("Response: status={}, method={}, path={}", response.getStatus(), request.getMethod(), request.getRequestURI());
                } else if (response.getStatus() < 500) {
                    log.warn("Response: status={}, method={}, path={}", response.getStatus(), request.getMethod(), request.getRequestURI());
                } else {
                    log.error("Response: status={}, method={}, path={}", response.getStatus(), request.getMethod(), request.getRequestURI());
                }
            }
            MDC.clear();
        }
    }

    public abstract boolean isRequestIgnored(HttpServletRequest request);

    public abstract Map<String, String> getAdditionalContextElements();

}