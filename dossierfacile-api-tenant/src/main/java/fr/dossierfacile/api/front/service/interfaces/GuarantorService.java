package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;

public interface GuarantorService {
    void delete(Long id, Tenant tenant);
    void deleteAllGuaratorsAssociatedToTenant(Tenant tenant);

    Guarantor findById(Long id);
}
