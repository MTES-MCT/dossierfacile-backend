package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;

public interface KeycloakService {

    String createKeycloakUserAccountCreation(AccountForm accountForm, Owner owner);

    String createKeycloakFromExistingUser(User user, String password);

    void confirmKeycloakUser(String keycloakId);

    boolean isKeycloakUser(String keyCloakId);

    void logout(Owner owner);

    void deleteKeycloakUser(Owner owner);

    String getKeycloakId(String email);

    String createKeycloakUser(String email);

    void createKeyCloakPassword(String keycloakId, String password);
}
