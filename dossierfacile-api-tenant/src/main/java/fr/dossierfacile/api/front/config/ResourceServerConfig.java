package fr.dossierfacile.api.front.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;


@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .headers()
                    .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options","nosniff"))
                    .contentTypeOptions()
                .and()
                .xssProtection()
                .and()
                .cacheControl()
                .and()
                .httpStrictTransportSecurity()
                .maxAgeInSeconds(63072000)
                .includeSubDomains(true)
                .and()
                .contentSecurityPolicy("frame-ancestors 'none'; frame-src 'none'; child-src 'none'; upgrade-insecure-requests; default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; img-src 'self' data:; font-src 'self'; connect-src *.dossierfacile.fr *.dossierfacile.fr:*; base-uri 'self'; form-action 'none'; media-src 'none'; worker-src 'none'; manifest-src 'none'; prefetch-src 'none';")
                .and()
                .frameOptions()
                .sameOrigin()
                .and()
                .formLogin()
                .disable()
                .httpBasic()
                .disable()
                .csrf()
                .disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers("/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs", "/api/register/account", "/api/register/confirmAccount/**",
                        "/api/auth/**", "/api/user/forgotPassword", "/api/user/createPassword/**",
                        "/api/document/**", "/api/file/download/**", "/api/file/preview/**", "/api/application/full/**", "/api/application/light/**",
                        "/api/application/fullPdf/**", "/api/tenant/property/**",
                        "/api/support/email",
                        "/actuator/health")
                .permitAll()
                .antMatchers("/api-partner-linking/**").hasAuthority("SCOPE_api-partner-linking")
                .antMatchers("/api-partner/**").hasAuthority("SCOPE_api-partner")
                .antMatchers("/dfc/tenant/**").hasAuthority("SCOPE_dfc")
                .anyRequest().hasAuthority("SCOPE_dossier")
                .and()
                .oauth2ResourceServer()
                .jwt();
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization"));
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