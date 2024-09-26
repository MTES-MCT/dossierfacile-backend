package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface TenantCommonService {
    void deleteTenantData(Tenant tenant);

    Tenant findByKeycloakId(String keycloakId);

    Long getTenantRank(Long id);
}
