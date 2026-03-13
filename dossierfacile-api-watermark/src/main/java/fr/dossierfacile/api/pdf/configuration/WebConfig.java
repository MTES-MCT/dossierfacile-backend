package fr.dossierfacile.api.pdf.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OWASP File Upload — "Protect the file upload from CSRF attacks" (CORS restriction reduces attack surface).
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = "*".equals(allowedOrigins)
                        ? new String[]{"*"}
                        : allowedOrigins.split("\\s*,\\s*");
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("Content-Type", "Accept")
                        .maxAge(3600);
            }
        };
    }
}
