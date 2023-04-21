package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
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
                    .map(userResource -> userResource.toRepresentation())
                    .orElse(null);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public String createKeycloakUserAccountCreation(AccountForm accountForm, Tenant tenant) {
        if (tenant.getKeycloakId() != null) {
            realmResource.users().delete(tenant.getKeycloakId());
        }
        var email = accountForm.getEmail().toLowerCase();
        var userRepresentation = createUser(email);
        createCredential(userRepresentation, accountForm.getPassword());
        return createUserAndReturnId(userRepresentation);
    }

    @Override
    public String createKeycloakUser(String email) {
        var userRepresentation = createUser(email);
        return createUserAndReturnId(userRepresentation);
    }

    @Override
    public void deleteKeycloakUsers(List<Tenant> tenants) {
        keycloakCommonService.deleteKeycloakUsers(tenants);
    }

    @Override
    public void confirmKeycloakUser(String keycloakId) {
        var userRepresentation = realmResource.users().get(keycloakId).toRepresentation();
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        realmResource.users().get(keycloakId).update(userRepresentation);
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
    public void deleteKeycloakUser(Tenant tenant) {
        keycloakCommonService.deleteKeycloakUser(tenant);
    }

    @Override
    public boolean isKeycloakUser(String keyCloakId) {
        return realmResource.users().get(keyCloakId) != null;
    }

    @Override
    public String getKeycloakId(String email) {
        var userRepresentations = realmResource.users().search(email);
        return userRepresentations.isEmpty() ? createKeycloakUser(email) : userRepresentations.get(0).getId();
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

}
