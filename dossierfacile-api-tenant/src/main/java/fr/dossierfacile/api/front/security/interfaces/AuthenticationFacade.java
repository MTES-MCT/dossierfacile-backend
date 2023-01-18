package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.common.entity.Tenant;

public interface AuthenticationFacade {
    String getKeycloakUserId();

    Tenant getLoggedTenant();

    Tenant getTenant(Long id);

    String getKeycloakClientId();

    String getFranceConnectLink(String redirectUri);

    KeycloakUser getKeycloakUser();
}
