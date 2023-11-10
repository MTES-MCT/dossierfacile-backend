package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.common.entity.Tenant;

public interface AuthenticationFacade {
    String getKeycloakClientId();

    String getUserEmail();

    String getKeycloakUserId();

    KeycloakUser getKeycloakUser();

    Tenant getLoggedTenant();

    Tenant getLoggedTenant(AcquisitionData acquisitionData);

    Tenant getTenant(Long id);

    String getFranceConnectLink(String redirectUri);
}
