package fr.gouv.bo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for testing endpoints.
 * Only active in dev, preprod, and test environments.
 * Explicitly blocked in production.
 */
@Configuration
@Profile({"dev", "preprod", "test", "!prod"})
public class TestingSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testingApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/testing/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
