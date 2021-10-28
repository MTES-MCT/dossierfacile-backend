package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class KeycloakService {

    private final RealmResource realmResource;

    public void deleteKeycloakSingleUser(Tenant tenant){
        realmResource.users().delete(tenant.getKeycloakId());
    }

    public void deleteKeycloakUsers(List<Tenant> tenants) {
        tenants.forEach(t -> {
            if(t.getKeycloakId()!=null){
                realmResource.users().delete(t.getKeycloakId());
            }
        });
    }

}
