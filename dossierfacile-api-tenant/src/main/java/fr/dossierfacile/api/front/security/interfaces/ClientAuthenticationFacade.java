package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.common.entity.UserApi;

import java.util.Optional;

public interface ClientAuthenticationFacade {
    String getKeycloakClientId();

    UserApi getClient();

    Optional<Integer> getApiVersion();

}