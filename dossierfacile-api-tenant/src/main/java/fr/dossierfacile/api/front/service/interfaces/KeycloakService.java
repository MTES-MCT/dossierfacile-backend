package fr.dossierfacile.api.front.service.interfaces;

import org.keycloak.representations.idm.UserRepresentation;

public interface KeycloakService {

    UserRepresentation getKeyCloakUser(String keycloakId);

    /**
     * @return the validated identity created or resolved for this email
     */
    String createKeycloakUser(String email);

    /**
     * @return false if the bound identity is missing or does not match the expected email;
     *         Keycloak service and credential-update errors propagate to the caller.
     */
    boolean createPasswordForBoundUser(String keycloakId, String email, String password);

    void deleteKeycloakUserById(String keycloakId);

    void logout(String keycloakUserId);

    void disableAccount(String keycloakId);

}
