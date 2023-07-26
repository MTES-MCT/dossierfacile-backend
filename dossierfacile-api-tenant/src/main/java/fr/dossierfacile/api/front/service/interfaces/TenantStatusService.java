package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface TenantStatusService {
    Tenant updateTenantStatus(Tenant tenant);
}
