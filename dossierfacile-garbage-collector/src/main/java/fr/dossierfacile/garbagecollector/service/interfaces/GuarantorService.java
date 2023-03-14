package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

public interface GuarantorService {
    void deleteAllGuaratorsAssociatedToTenant(Tenant tenant);
}
