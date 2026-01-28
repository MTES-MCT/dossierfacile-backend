package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {
    private final RealmResource realmResource;
    private final KeycloakCommonService keycloakCommonService;

    @Override
    public UserRepresentation getKeyCloakUser(String keycloakId) {
        try {
            return Optional.ofNullable(keycloakId)
                    .map(kid -> realmResource.users().get(kid))
                    .map(UserResource::toRepresentation)
                    .orElse(null);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public String createKeycloakUser(String email) {
        var userRepresentation = createUser(email);
        return createUserAndReturnId(userRepresentation);
    }

    @Override
    public void createKeyCloakPassword(String keycloakId, String password) {
        var userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        createCredential(userRepresentation, password);
        realmResource.users().get(keycloakId).update(userRepresentation);
    }

    @Override
    public void deleteKeycloakUserById(String keycloakId) {
        keycloakCommonService.deleteKeycloakUserById(keycloakId);
    }

    @Override
    public String getKeycloakId(String email) {
        var userRepresentations = realmResource.users().search(email);
        return userRepresentations.isEmpty() ? createKeycloakUser(email) : userRepresentations.getFirst().getId();
    }

    @Override
    public void logout(String keycloakUserId) {
        realmResource.users().get(keycloakUserId).logout();
    }

    @Override
    public void unlinkFranceConnect(Tenant tenant) {
        var userRepresentation = realmResource.users().get(tenant.getKeycloakId()).toRepresentation();
        userRepresentation.singleAttribute("france-connect", "false");
        realmResource.users().get(tenant.getKeycloakId()).update(userRepresentation);
    }

    @Override
    public void disableAccount(String keycloakId) {
        var userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
        userRepresentation.setEnabled(false);
        userRepresentation.setEmailVerified(false);
        realmResource.users().get(keycloakId).update(userRepresentation);
    }

    private UserRepresentation createUser(String email) {
        var userRepresentation = new UserRepresentation();
        userRepresentation.setEmail(email);
        userRepresentation.setUsername(email);
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(false);
        return userRepresentation;
    }

    private void createCredential(UserRepresentation userRepresentation, String password) {
        var credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(password);
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));
    }

    private String createUserAndReturnId(UserRepresentation userRepresentation) {
        try (var response = realmResource.users().create(userRepresentation)) {
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                var keycloakUser = realmResource.users().search(userRepresentation.getEmail()).getFirst();
                return keycloakUser.getId();
            }
            var split = response.getHeaderString("Location").split("/");
            return split[split.length - 1];
        }
    }

}
