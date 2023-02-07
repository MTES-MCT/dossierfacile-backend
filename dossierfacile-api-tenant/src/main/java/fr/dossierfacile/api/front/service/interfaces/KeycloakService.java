package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface KeycloakService {

    String createKeycloakUserAccountCreation(AccountForm accountForm, Tenant tenant);

    /**
     * @return created user's keycloakId
     */
    String createKeycloakUser(String email);

    void deleteKeycloakUsers(List<Tenant> tenants);

    void confirmKeycloakUser(String keycloakId);

    void createKeyCloakPassword(String keycloakId, String password);

    void deleteKeycloakUser(Tenant tenant);

    boolean isKeycloakUser(String keyCloakId);

    String getKeycloakId(String email);

    void logout(String keycloakUserId);

    void unlinkFranceConnect(Tenant tenant);

    void disableAccount(String keycloakId);
}
