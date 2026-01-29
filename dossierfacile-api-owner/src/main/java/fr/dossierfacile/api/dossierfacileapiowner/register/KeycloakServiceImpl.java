package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import jakarta.ws.rs.NotFoundException;
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
    public String createKeycloakFromExistingUser(User user, String password) {
        var email = user.getEmail();
        var userRepresentation = buildKeycloakUser(email);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        createCredential(userRepresentation, password);
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
        log.info("Trying to create user in keycloak: {}", userRepresentation);
        try (var response = realmResource.users().create(userRepresentation)) {
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                log.warn("User already exists in keycloak, searching for user by email: {}", userRepresentation.getUsername());
                var keycloakUser = realmResource.users().search(userRepresentation.getUsername()).get(0);
                return keycloakUser.getId();
            }
            log.info("User created in keycloak, returning id: {}", response.getHeaderString("Location"));
            var split = response.getHeaderString("Location").split("/");
            return split[split.length - 1];
        }
    }

    @Override
    public void deleteKeycloakUser(Owner owner) {
        realmResource.users().delete(owner.getKeycloakId());
    }

    @Override
    public String getKeycloakId(String email) {
        var userRepresentations = realmResource.users().search(email);
        return userRepresentations.isEmpty() ? null : userRepresentations.get(0).getId();
    }

    @Override
    public String createKeycloakUser(String email) {
        var userRepresentation = buildKeycloakUser(email);
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
}
