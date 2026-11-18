package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;

public interface KeycloakService {

    String createKeycloakUserAccountCreation(AccountForm accountForm, Owner owner);

    void confirmKeycloakUser(User user);

    boolean isKeycloakUser(String keyCloakId);

    void logout(Owner owner);

    void deleteKeycloakUser(Owner owner);

    String createKeycloakUser(String email);

    /**
     * @return false if the bound identity is missing or does not match the expected email;
     *         Keycloak service and credential-update errors propagate to the caller.
     */
    boolean createPasswordForBoundUser(String keycloakId, String email, String password);
}
