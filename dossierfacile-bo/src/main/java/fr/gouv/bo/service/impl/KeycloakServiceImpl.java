package fr.gouv.bo.service.impl;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.service.KeycloakService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                    .map(UserResource::toRepresentation)
                    .orElse(null);
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve user from keycloak", e);
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

    @Override
    public void createKeycloakClient(UserApi userApi) {

        String clientTemplateName = (userApi.getName().startsWith("dfconnect-")) ? "dfconnect-template" :
                ((userApi.getName().startsWith("hybrid-")) ? "hybrid-template" : null);

        ClientRepresentation clientTemplate = realmResource.clients()
                .findByClientId(clientTemplateName).get(0);

        clientTemplate.setId(null);
        clientTemplate.setClientId(userApi.getName());
        clientTemplate.setDescription("Client DFC pour " + userApi.getName2() + " créé le " + LocalDateTime.now());
        clientTemplate.setName(userApi.getName2());
        clientTemplate.setSecret(null);
        clientTemplate.getProtocolMappers().stream().forEach((pm) -> pm.setId(null));
        clientTemplate.setAttributes(null);
        clientTemplate.setEnabled(true);

        Response response = realmResource.clients().create(clientTemplate);
        log.info(" status =  " + response.getStatus());
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            throw new IllegalStateException("Status de retour création de client " + response.getStatus());
        }
    }

    @Override
    public ClientRepresentation getKeyCloakClient(String clientName) {
        return realmResource.clients().findByClientId(clientName).get(0);
    }

}
