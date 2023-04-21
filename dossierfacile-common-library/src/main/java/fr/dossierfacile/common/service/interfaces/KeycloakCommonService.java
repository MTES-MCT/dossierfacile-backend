package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface KeycloakCommonService {

    void deleteKeycloakUsers(List<Tenant> tenants);

    void deleteKeycloakUser(Tenant tenant);

}
