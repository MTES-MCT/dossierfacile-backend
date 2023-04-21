package fr.dossierfacile.process.file.configuration;

import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DummyConfig {
    @Bean
    public Optional<RealmResource> geRealmResource() {
        return Optional.empty();
    }
}
