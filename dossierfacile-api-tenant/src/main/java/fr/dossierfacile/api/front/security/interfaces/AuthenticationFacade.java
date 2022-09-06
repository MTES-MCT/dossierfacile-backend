package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface AuthenticationFacade {

    String getKeycloakUserId();

    Tenant getTenant(Long id);

    String getKeycloakClientId();

    String getFranceConnectOauth(Tenant tenant, String redirectUri);
}
