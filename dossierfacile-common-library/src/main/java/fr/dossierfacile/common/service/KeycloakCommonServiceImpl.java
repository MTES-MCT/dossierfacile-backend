package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class KeycloakCommonServiceImpl implements KeycloakCommonService {

    private final Optional<RealmResource> realmResource;

    @Override
    public void deleteKeycloakUsers(List<User> users) {
        if (realmResource.isEmpty()) {
            return;
        }
        users.stream().filter(u -> u.getKeycloakId() != null).forEach(u -> {
            try (Response response = realmResource.get().users().delete(u.getKeycloakId())) {
                checkResult(response);
            }
        });
    }

    @Override
    public void deleteKeycloakUser(User user) {
        deleteKeycloakUsers(Collections.singletonList(user));
    }

    @Override
    public void deleteKeycloakUserById(String keycloakId) {
        if (realmResource.isEmpty()) {
            return;
        }
        try (Response response = realmResource.get().users().delete(keycloakId)) {
            checkResult(response);
        }
    }

    @Override
    public void revokeUserConsent(Tenant tenant, UserApi userApi) {
        if (realmResource.isEmpty()) {
            return;
        }
        String keycloakId = tenant.getKeycloakId();
        UserResource user = realmResource.get().users().get(keycloakId);
        user.revokeConsent(userApi.getName());
    }

    private static void checkResult(Response response) {
        if (response.getStatus() != 204) {
            log.warn("Failed to delete user. Status: {}", response.getStatus());
        }
    }

}
