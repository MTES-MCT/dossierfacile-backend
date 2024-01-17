package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeycloakService {
    UserRepresentation getKeyCloakUser(String keycloakId);

    void deleteKeycloakSingleUser(User tenant);

    void deleteKeycloakUsers(List<User> tenants);

    void createKeycloakClient(UserApi userApi);
}
