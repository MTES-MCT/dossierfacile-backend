package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.PartnerCallBackType;

public interface PartnerCallBackService {

    void sendCallBack(Tenant tenant);
    void registerTenant(String internalPartnerId, Tenant tenant, UserApi userApi);
    void sendCallBack(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType);
}
