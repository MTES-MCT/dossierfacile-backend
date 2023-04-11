package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface TenantCommonService {
    void recordAndDeleteTenantData(Tenant tenant);
}
