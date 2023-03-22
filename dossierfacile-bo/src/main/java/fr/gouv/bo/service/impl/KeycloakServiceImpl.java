package fr.gouv.bo.service.impl;

import fr.dossierfacile.common.entity.User;
import fr.gouv.bo.service.KeycloakService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final RealmResource realmResource;

    @Override
    public UserRepresentation getKeyCloakUser(String keycloakId) {
        try {
            return Optional.ofNullable(keycloakId)
                    .map(kid -> realmResource.users().get(kid))
                    .map(userResource -> userResource.toRepresentation())
                    .orElse(null);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public void deleteKeycloakSingleUser(User tenant) {
        if (tenant.getKeycloakId() != null) {
            realmResource.users().delete(tenant.getKeycloakId());
        } else {
            log.warn("Cannot remove keycloak user because missing : User with id " + tenant.getId() + " has not associated keycloak user");
        }
    }

    @Override
    public void deleteKeycloakUsers(List<User> tenants) {
        tenants.forEach(t -> deleteKeycloakSingleUser(t));
    }
}
