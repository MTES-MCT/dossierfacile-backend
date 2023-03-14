package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface TenantWarningService {
    void handleTenantWarning(Tenant t, int warnings);
}
