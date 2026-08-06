package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface TenantStatusNotificationService {
    void notifyTenantDeclined(Tenant tenant);
    void notifyTenantValidated(Tenant tenant);
}
