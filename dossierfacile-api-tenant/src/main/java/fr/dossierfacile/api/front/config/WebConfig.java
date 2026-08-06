package fr.dossierfacile.api.front.config;

import fr.dossierfacile.api.front.security.KeycloakIdResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuration MVC permettant d'ajouter le résolveur KeycloakIdResolver.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new KeycloakIdResolver());
    }
}
