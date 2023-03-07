package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface DocumentService {
    void deleteAllDocumentsAssociatedToTenant(Tenant tenant);
}
