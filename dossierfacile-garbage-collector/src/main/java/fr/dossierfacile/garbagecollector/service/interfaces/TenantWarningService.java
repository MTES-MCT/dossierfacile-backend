package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

import java.time.LocalDateTime;
import java.util.List;

public interface TenantWarningService {
    void handleTenantWarning(Tenant t, int warnings);

    void deleteOldArchivedWarnings(List<Tenant> tenantList);
}
