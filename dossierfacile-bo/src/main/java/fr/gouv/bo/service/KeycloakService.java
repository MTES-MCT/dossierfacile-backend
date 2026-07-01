package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Optional;

public interface KeycloakService {
    UserRepresentation getKeyCloakUser(String keycloakId);

    Optional<UserRepresentation> findUserByEmail(String email);

    boolean markEmailAsVerified(String email);

    boolean deleteKeycloakUserByEmail(String email);

    void deleteKeycloakSingleUser(User tenant);

    void deleteKeycloakUsers(List<User> tenants);

    void createKeycloakClient(UserApi userApi);

    ClientRepresentation getKeyCloakClient(String name);
}
