package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;

public interface AuthenticationFacade {

    String getKeycloakUserId();

    Owner getOwner();

    String getKeycloakClientId();
}
