package fr.dossierfacile.api.front.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Slf4j
public class ConnectionContextFilter implements Filter {
    private static final String URI = "uri";
    private static final String CLIENT_ID = "client";
    private static final String KC_ID = "user";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            MDC.put(URI, httpServletRequest.getRequestURI());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                MDC.put(CLIENT_ID, ((Jwt) authentication.getPrincipal()).getClaimAsString("azp"));
                MDC.put(KC_ID, ((Jwt) authentication.getPrincipal()).getClaimAsString("sub"));
            }
        } catch (Exception e) {
            // Something wrong but service should stay up
            log.error("Unable to inject data in MDC !!!");
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(URI);
            MDC.remove(CLIENT_ID);
            MDC.remove(KC_ID);
        }
    }
}