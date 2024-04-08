package fr.dossierfacile.api.front.config;


import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi dfcOpenApi() {
        return GroupedOpenApi.builder().displayName("API DFC").group("dfc").pathsToMatch("/dfc/**").build();
    }

    @Bean
    public GroupedOpenApi partnerOpenApi() {
        return GroupedOpenApi.builder().displayName("API Partner").group("api-partner").pathsToMatch("/api-partner/**").build();
    }
}
