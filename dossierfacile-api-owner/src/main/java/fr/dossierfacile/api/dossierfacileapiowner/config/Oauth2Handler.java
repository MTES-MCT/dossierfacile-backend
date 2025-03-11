package fr.dossierfacile.api.dossierfacileapiowner.config;

import ch.qos.logback.classic.Level;
import fr.dossierfacile.common.utils.LoggerUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.util.Collections;

// This class can not be included inside the common lib. Because we need to include the dependency to the spring-security-web
// witch cause issue on other project that don't use spring-security-web

@Slf4j
@Configuration
public class Oauth2Handler {

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            LoggerUtil.prepareMDCForHttpRequest(request, Collections.emptyMap());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            LoggerUtil.addRequestStatusToMdc(HttpServletResponse.SC_UNAUTHORIZED);

            var logMessage = String.format(
                    "Request completed: URI:%s, Method:%s, Status:%d",
                    request.getRequestURI(),
                    request.getMethod(),
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            LoggerUtil.sendEnrichedLogs(Level.WARN, logMessage);
        };
    }

}
