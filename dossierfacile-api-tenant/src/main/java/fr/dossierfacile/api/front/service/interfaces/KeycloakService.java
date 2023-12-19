package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeycloakService {

    UserRepresentation getKeyCloakUser(String keycloakId);

    String createKeycloakUserAccountCreation(AccountForm accountForm, Tenant tenant);

    /**
     * @return created user's keycloakId
     */
    String createKeycloakUser(String email);

    void deleteKeycloakUsers(List<Tenant> tenants);

    void confirmKeycloakUser(String keycloakId);

    void createKeyCloakPassword(String keycloakId, String password);

    void deleteKeycloakUser(Tenant tenant);

    String getKeycloakId(String email);

    void logout(String keycloakUserId);

    void unlinkFranceConnect(Tenant tenant);

    void disableAccount(String keycloakId);

    void revokeUserConsent(Tenant tenant, UserApi userApi);

}
