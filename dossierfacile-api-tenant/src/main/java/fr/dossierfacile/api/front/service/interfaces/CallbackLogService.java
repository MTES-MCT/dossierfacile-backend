package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.LightAPIInfoModel;
import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;

public interface CallbackLogService {
    void createCallbackLogForInternalPartnerLight(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, LightAPIInfoModel lightAPIInfoModel);

    void createCallbackLogForPartnerModel(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, ApplicationModel applicationModel);
}
