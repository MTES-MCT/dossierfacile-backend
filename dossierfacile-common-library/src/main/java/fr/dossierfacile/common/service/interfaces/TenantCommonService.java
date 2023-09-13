package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;

public interface TenantCommonService {
    void deleteTenantData(Tenant tenant);

    Tenant findByKeycloakId(String keycloakId);

}
