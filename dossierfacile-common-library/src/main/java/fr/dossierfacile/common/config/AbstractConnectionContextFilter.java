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
    private static final String REAL_IP = "ip";
    private static final String JAKARTA_SERVLET_FORWARD_REQUEST_URI = "jakarta.servlet.forward.request_uri";
    private static final String JAKARTA_SERVLET_ERROR_MESSAGE = "jakarta.servlet.error.message";
    private static final String JAKARTA_SERVLET_ERROR_EXCEPTION = "jakarta.servlet.error.exception";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isRequestIgnored(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            MDC.put(URI, request.getRequestURI());
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
            MDC.put(REAL_IP, request.getHeader("X-Real-Ip")); // specific to Scalingo infra

            getAdditionalContextElements().forEach(MDC::put);

            String requestUri = request.getRequestURI();
            if ("/error".equals(requestUri) && request.getAttribute(JAKARTA_SERVLET_FORWARD_REQUEST_URI) != null) {
                String previousRequestUri = request.getAttribute(JAKARTA_SERVLET_FORWARD_REQUEST_URI).toString();
                Object errorMessage = request.getAttribute(JAKARTA_SERVLET_ERROR_MESSAGE);
                Object errorException = request.getAttribute(JAKARTA_SERVLET_ERROR_EXCEPTION);
                Object bestMatchingHandler = request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingHandler");
                log.info("Request: method={}, path={}, previousRequestUri={}, errorMessage={}, errorException={}, bestMatchingHandler={}", request.getMethod(), requestUri, previousRequestUri, errorMessage, errorException, bestMatchingHandler);
            } else {
                log.info("Request: method={}, path={}", request.getMethod(), requestUri);
            }
        } catch (Exception e) {
            // Something wrong but service should stay up
            log.warn("Unable to inject data in MDC", e);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (response.getStatus() != 200) {
                MDC.put(RESPONSE_STATUS, String.valueOf(response.getStatus()));
                if ("/error".equals(request.getRequestURI()) && request.getAttribute(JAKARTA_SERVLET_FORWARD_REQUEST_URI) != null) {
                    String previousRequestUri = request.getAttribute(JAKARTA_SERVLET_FORWARD_REQUEST_URI).toString();
                    log.info("Response: status={}, method={}, path={}, previousRequestUri={}", response.getStatus(), request.getMethod(), request.getRequestURI(), previousRequestUri);
                } else if (response.getStatus() < 400) {
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