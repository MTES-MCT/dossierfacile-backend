package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;

public interface KeycloakService {

    String createKeycloakUserAccountCreation(AccountForm accountForm, Owner owner);

    void confirmKeycloakUser(String keycloakId);

    boolean isKeycloakUser(String keyCloakId);

    void logout(Owner owner);
}
