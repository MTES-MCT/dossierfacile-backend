package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class KeycloakCommonServiceImpl implements KeycloakCommonService {
    private final RealmResource realmResource;

    @Override
    public void deleteKeycloakUsers(List<Tenant> tenants) {
        tenants.stream().filter(t -> t.getKeycloakId() != null).forEach(t -> realmResource.users().delete(t.getKeycloakId()));
    }

    @Override
    public void deleteKeycloakUser(Tenant tenant) {
        if (tenant.getKeycloakId() != null) {
            realmResource.users().delete(tenant.getKeycloakId());
        } else {
            log.warn("Trying to delete tenant without keycloakId:" + tenant.getEmail());
        }
    }

}
