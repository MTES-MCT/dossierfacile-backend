package fr.dossierfacile.api.front.config;

import fr.dossierfacile.api.front.config.filter.ConnectionContextFilter;
import fr.dossierfacile.api.front.security.PartnerAuthorizationManager;
import fr.dossierfacile.logging.request.Oauth2LoggingHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private static final List<String> PERMIT_ALL_PATTERNS = Arrays.asList(
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/api/register/account", "/api/tenant/doNotArchive/**",
            "/api/auth/**", "/api/user/forgotPassword", "/api/user/createPassword/**",
            "/api/document/**", "/api/application/full/**", "/api/application/light/**",
            "/api/application/fullPdf/**", "/api/tenant/property/**",
            "/api/support/email",
            "/api/stats/**",
            "/api/onetimesecret/**",
            "/actuator/health"
    );

    private final AuthenticationEntryPoint authenticationEntryPoint = createAuthenticationEntryPoint();

    @Value("${resource.server.config.csp}")
    private String configCsp;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(new ConnectionContextFilter(), FilterSecurityInterceptor.class)
                .headers(headers -> headers
                        .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                        .contentTypeOptions(withDefaults())
                        .xssProtection(withDefaults())
                        .cacheControl(withDefaults())
                        .httpStrictTransportSecurity(transport -> transport.maxAgeInSeconds(63072000).includeSubDomains(true))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(configCsp))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATTERNS.toArray(String[]::new)).permitAll()
                        .requestMatchers("/api-partner/**").access(apiPartnerAuthorizationManager())
                        .requestMatchers("/dfc/api/**").access(dfcPartnerServiceAuthorizationManager())
                        .requestMatchers("/dfc/**").hasAuthority("SCOPE_dfc")
                        .anyRequest().hasAuthority("SCOPE_dossier")
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(
                                                new JwtAuthenticationConverter()
                                        )
                                )
                                .authenticationEntryPoint(authenticationEntryPoint)
                );
        return http.build();
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> apiPartnerAuthorizationManager() {
        return new PartnerAuthorizationManager("api-partner");
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> dfcPartnerServiceAuthorizationManager() {
        return new PartnerAuthorizationManager("dfc");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization", "Baggage", "Sentry-trace", "Content-Disposition", "X-Tenant-Trigram"));
        configuration.setAllowedMethods(Arrays.asList("DELETE", "GET", "POST", "PATCH", "PUT"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * This prevents the OAuth2 resource server from returning 401 for /api/application/full/** and /api/application/light/**
     * endpoints, allowing controller exceptions (400/403) to be properly handled by exception handlers.
     * 
     * This logic only applies to these specific routes to avoid impacting error handling for other permitAll routes.
     */
    private AuthenticationEntryPoint createAuthenticationEntryPoint() {
        final AuthenticationEntryPoint delegate = new Oauth2LoggingHandler();
        
        // Specific routes where we want to allow controller exceptions to be handled normally
        final List<String> APPLICATION_LINKS_PATTERNS = Arrays.asList(
                "/api/application/full/**",
                "/api/application/light/**"
        );
        
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                                AuthenticationException authException) throws IOException, ServletException {
                // Check if the request matches the specific application routes
                for (String pattern : APPLICATION_LINKS_PATTERNS) {
                    AntPathRequestMatcher matcher = new AntPathRequestMatcher(pattern);
                    if (matcher.matches(request)) {
                        // For these specific endpoints, don't set any response status
                        // This allows the request to proceed and controller exceptions to be handled normally
                        // The response will be handled by the controller or exception handlers
                        return;
                    }
                }
                
                // For all other endpoints (including other permitAll routes), use the delegate entry point
                delegate.commence(request, response, authException);
            }
        };
    }
}
