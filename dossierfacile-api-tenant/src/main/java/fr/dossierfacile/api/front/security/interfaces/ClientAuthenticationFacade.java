package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.common.entity.UserApi;

public interface ClientAuthenticationFacade {
    UserApi getClient();
}