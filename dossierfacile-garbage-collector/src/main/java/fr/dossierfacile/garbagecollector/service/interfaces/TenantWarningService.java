package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

import java.time.LocalDateTime;

public interface TenantWarningService {
    void handleTenantWarning(Tenant t, int warnings);

    void deleteOldArchivedWarnings(LocalDateTime limitDate);
}
