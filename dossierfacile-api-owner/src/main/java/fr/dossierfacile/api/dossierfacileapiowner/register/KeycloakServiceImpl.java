package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.common.entity.Owner;
import lombok.AllArgsConstructor;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final RealmResource realmResource;

    @Override
    public String createKeycloakUserAccountCreation(AccountForm accountForm, Owner owner) {
        if (owner.getKeycloakId() != null) {
            realmResource.users().delete(owner.getKeycloakId());
        }
        var email = accountForm.getEmail();
        var userRepresentation = createUser(email);
        createCredential(userRepresentation, accountForm.getPassword());
        return createUserAndReturnId(userRepresentation);
    }

    @Override
    public void confirmKeycloakUser(String keycloakId) {
        var userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
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

    private UserRepresentation createUser(String email) {
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

    private String createUserAndReturnId(UserRepresentation userRepresentation) {
        try (var response = realmResource.users().create(userRepresentation)) {
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                var keycloakUser = realmResource.users().search(userRepresentation.getEmail()).get(0);
                return keycloakUser.getId();
            }
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
        return userRepresentations.isEmpty() ? createKeycloakUser(email) : userRepresentations.get(0).getId();
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
}
