package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
        return createUserAndReturnId(createUser(email));
    }

    @Override
    public boolean createPasswordForBoundUser(String keycloakId, String email, String password) {
        if (StringUtils.isBlank(keycloakId) || StringUtils.isBlank(email)) {
            return false;
        }

        UserResource userResource;
        UserRepresentation userRepresentation;
        try {
            userResource = realmResource.users().get(keycloakId);
            userRepresentation = userResource.toRepresentation();
        } catch (NotFoundException e) {
            return false;
        }

        if (userRepresentation == null || !Strings.CS.equals(keycloakId, userRepresentation.getId()) ||
                !Strings.CI.equals(email, userRepresentation.getEmail())) {
            return false;
        }

        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        createCredential(userRepresentation, password);
        userResource.update(userRepresentation);
        return true;
    }

    @Override
    public void deleteKeycloakUserById(String keycloakId) {
        keycloakCommonService.deleteKeycloakUserById(keycloakId);
    }

    @Override
    public void logout(String keycloakUserId) {
        realmResource.users().get(keycloakUserId).logout();
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
        String email = userRepresentation.getEmail();
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("An email is required to provision a Keycloak identity");
        }
        try (var response = realmResource.users().create(userRepresentation)) {
            String keycloakId;
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                var results = realmResource.users().searchByEmail(email, true);
                var matches = results == null
                        ? Collections.<UserRepresentation>emptyList()
                        : results.stream()
                                .filter(result -> result != null && Strings.CI.equals(email, result.getEmail()))
                                .toList();
                if (matches.size() != 1 || StringUtils.isBlank(matches.getFirst().getId())) {
                    throw new IllegalStateException("Cannot resolve a unique Keycloak identity");
                }
                keycloakId = matches.getFirst().getId();
            } else if (response.getStatus() == HttpStatus.CREATED.value()) {
                keycloakId = createdUserId(response.getLocation());
            } else {
                throw new IllegalStateException("Unexpected Keycloak user creation status: " + response.getStatus());
            }
            requireMatchingUser(keycloakId, email);
            return keycloakId;
        }
    }

    private String createdUserId(java.net.URI location) {
        if (location == null || location.getPath() == null || location.getQuery() != null || location.getFragment() != null) {
            throw new IllegalStateException("Invalid Keycloak user creation Location");
        }
        String path = location.getPath();
        int usersSegment = path.lastIndexOf("/users/");
        if (usersSegment < 0) {
            throw new IllegalStateException("Invalid Keycloak user creation Location");
        }
        String id = path.substring(usersSegment + "/users/".length());
        if (StringUtils.isBlank(id) || id.contains("/") || id.equals(".") || id.equals("..")) {
            throw new IllegalStateException("Invalid Keycloak user creation Location");
        }
        return id;
    }

    private void requireMatchingUser(String keycloakId, String email) {
        UserRepresentation identity = getKeyCloakUser(keycloakId);
        if (identity == null || !Strings.CS.equals(keycloakId, identity.getId()) || !Strings.CI.equals(email, identity.getEmail())) {
            throw new IllegalStateException("Keycloak identity does not match the expected account");
        }
    }

}
