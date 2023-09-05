package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

public interface CallbackLogService {
    void createCallbackLogForPartnerModel(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, ApplicationModel applicationModel);
}
