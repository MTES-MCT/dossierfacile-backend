package fr.dossierfacile.logging.request;

import ch.qos.logback.classic.Level;
import fr.dossierfacile.logging.util.LoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Collections;

public class Oauth2LoggingHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        LoggerUtil.prepareMDCForHttpRequest(request, Collections.emptyMap());
        LoggerUtil.setNormalizedUri(request);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        LoggerUtil.addRequestStatusToMdc(HttpServletResponse.SC_UNAUTHORIZED);

        var logMessage = String.format(
                "Request completed: URI:%s, Method:%s, Status:%d, Ip: %s, User-Agent: %s, Referer: %s",
                request.getRequestURI(),
                request.getMethod(),
                HttpServletResponse.SC_UNAUTHORIZED,
                request.getHeader("X-Real-Ip"),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );

        LoggerUtil.sendEnrichedLogs(Level.WARN, logMessage);
    }
}
