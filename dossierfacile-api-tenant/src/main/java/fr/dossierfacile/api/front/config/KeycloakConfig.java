package fr.dossierfacile.api.front.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Value("${keycloak.server.url}")
    private String keycloakServerUrl;
    @Value("${keycloak.server.realm}")
    private String keycloakRealm;
    @Value("${keycloak.server.client.secret}")
    private String clientSecret;
    @Value("${keycloak.server.client.id}")
    private String clientId;

    @Bean
    public RealmResource geRealmResource() {
        var keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .realm("master")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        return keycloak.realm(keycloakRealm);
    }
}
