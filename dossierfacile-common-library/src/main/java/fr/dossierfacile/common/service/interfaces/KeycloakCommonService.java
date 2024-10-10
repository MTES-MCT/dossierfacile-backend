package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;

import java.util.List;

public interface KeycloakCommonService {

    void deleteKeycloakUsers(List<User> users);

    void deleteKeycloakUser(User user);

    void deleteKeycloakUserById(String keycloakId);
}
