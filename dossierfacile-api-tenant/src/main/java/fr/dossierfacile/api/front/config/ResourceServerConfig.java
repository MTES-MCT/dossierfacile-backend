package fr.dossierfacile.api.front.config;

import fr.dossierfacile.api.front.config.filter.ConnectionContextFilter;
import fr.dossierfacile.api.front.security.CustomWebSecurityExpressionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

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
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'; frame-src 'none'; child-src 'none'; upgrade-insecure-requests; default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; img-src 'self' data:; font-src 'self'; connect-src *.dossierfacile.fr *.dossierfacile.fr:*; base-uri 'self'; form-action 'none'; media-src 'none'; worker-src 'none'; manifest-src 'none'; prefetch-src 'none';"))
                        .frameOptions(FrameOptionsConfig::sameOrigin)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/api/register/account", "/api/register/confirmAccount/**",
                                "/api/auth/**", "/api/user/forgotPassword", "/api/user/createPassword/**",
                                "/api/document/**", "/api/application/full/**", "/api/application/light/**",
                                "/api/application/fullPdf/**", "/api/tenant/property/**",
                                "/api/support/email",
                                "/api/stats/**",
                                "/api/onetimesecret/**",
                        "/actuator/health").permitAll()
                        .requestMatchers("/api-partner/**").access(new WebExpressionAuthorizationManager("hasAuthority(\"SCOPE_api-partner\") && isClient()"))
                        .requestMatchers("/dfc/**").hasAuthority("SCOPE_dfc")
                        .requestMatchers("/dfc/api/**").access(new WebExpressionAuthorizationManager("isClient()"))
                        .anyRequest().hasAuthority("SCOPE_dossier")
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);

        return http.build();
    }

    @Bean
    public SecurityExpressionHandler<FilterInvocation> customWebSecurityExpressionHandler() {
        return new CustomWebSecurityExpressionHandler();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization", "Baggage", "Sentry-trace"));
        configuration.setAllowedMethods(Arrays.asList("DELETE", "GET", "POST", "PATCH", "PUT"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}