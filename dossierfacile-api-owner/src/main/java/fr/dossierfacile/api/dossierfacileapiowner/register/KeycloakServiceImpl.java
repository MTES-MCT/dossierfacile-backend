package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.keycloak.admin.client.resource.UserResource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final RealmResource realmResource;
    private final UserRepository userRepository;
    @Override
    public String createKeycloakUserAccountCreation(AccountForm accountForm, Owner owner) {
        if (owner.getKeycloakId() != null) {
            realmResource.users().delete(owner.getKeycloakId());
        }
        var email = accountForm.getEmail();
        var userRepresentation = buildKeycloakUser(email);
        createCredential(userRepresentation, accountForm.getPassword());
        return createUserAndReturnId(userRepresentation);
    }

    @Override
    public void confirmKeycloakUser(User user) {
        String keycloakId = user.getKeycloakId();
        String email = user.getEmail();

        UserRepresentation userRepresentation = null;
        log.info("Confirming keycloak user: keycloakId: {}, email: {}", keycloakId, email);
        // catch jakarta.ws.rs.NotFoundException if the user does not exist in keycloak.
        // catch NullPointerException if the keycloakId is null in table user_account.
        // These cases can happen for old user accounts, an incomplete migration is suspected for account created before 2022.
        try {
            log.info("Getting user representation for keycloak ID: {}", keycloakId);
            userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
        } catch (NotFoundException | NullPointerException e) {
            log.warn("User not found in keycloak, creating new user or linking to existing user");
            keycloakId = createKeycloakUser(email);
            user.setKeycloakId(keycloakId);
            userRepository.save(user);

            log.info("Getting user representation for new keycloak ID: {}", keycloakId);
            userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
            log.info("User representation: {}", userRepresentation);
        }

        // In all cases, set the user as enabled and email verified
        log.info("Setting user as enabled and email verified");
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        realmResource.users().get(keycloakId).update(userRepresentation);
    }

    @Override
    public boolean isKeycloakUser(String keyCloakId) {
        return realmResource.users().get(keyCloakId) != null;
    }

    @Override
    public void logout(Owner owner) {
        realmResource.users().get(owner.getKeycloakId()).logout();
    }

    private UserRepresentation buildKeycloakUser(String email) {
        var userRepresentation = new UserRepresentation();
        userRepresentation.setEmail(email);
        userRepresentation.setUsername(email);
        userRepresentation.setEnabled(false);
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

    // Create the user in keycloak and return the id. If the user already exists, return the id of the existing user.
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
                throw new IllegalStateException("Unexpected Keycloak user creation status");
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
        UserRepresentation identity;
        try {
            identity = realmResource.users().get(keycloakId).toRepresentation();
        } catch (NotFoundException e) {
            throw new IllegalStateException("Keycloak identity does not match the expected account", e);
        }
        if (identity == null || !Strings.CS.equals(keycloakId, identity.getId()) || !Strings.CI.equals(email, identity.getEmail())) {
            throw new IllegalStateException("Keycloak identity does not match the expected account");
        }
    }

    @Override
    public void deleteKeycloakUser(Owner owner) {
        realmResource.users().delete(owner.getKeycloakId());
    }

    @Override
    public String createKeycloakUser(String email) {
        var userRepresentation = buildKeycloakUser(email);
        return createUserAndReturnId(userRepresentation);
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
}
