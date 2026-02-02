package fr.dossierfacile.api.front.config;


import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SwaggerConfig {

    @Bean
    @Profile("!dev")
    public GroupedOpenApi dfcOpenApi() {
        return GroupedOpenApi.builder().displayName("API DFC").group("dfc").pathsToMatch("/dfc/**").build();
    }

    @Bean
    @Profile("dev")
    public GroupedOpenApi allOpenApi() {
        return GroupedOpenApi.builder().displayName("All API").group("all").pathsToMatch("/**").build();
    }

    @Bean
    @Profile("dev")
    public GroupedOpenApi allOpenApi() {
        return GroupedOpenApi.builder().displayName("All API").group("all").pathsToMatch("/**").build();
    }
}
