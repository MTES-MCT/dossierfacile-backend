package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import org.keycloak.representations.idm.UserRepresentation;

public interface KeycloakService {

    UserRepresentation getKeyCloakUser(String keycloakId);

    /**
     * @return created user's keycloakId
     */
    String createKeycloakUser(String email);

    void createKeyCloakPassword(String keycloakId, String password);

    void deleteKeycloakUserById(String keycloakId);

    String getKeycloakId(String email);

    void logout(String keycloakUserId);

    void unlinkFranceConnect(Tenant tenant);

    void disableAccount(String keycloakId);

    void revokeUserConsent(Tenant tenant, UserApi userApi);

}
