package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;

import java.util.Map;

public interface MailCommonService {

    void sendEmailToTenant(UserDto tenant, Map<String, String> params, Long templateId);

    void sendEmailToTenantAfterValidateAllTenantForGroup(TenantDto tenant);

    void sendEmailToTenantAfterValidateAllDocuments(TenantDto tenant);

    void sendEmailToTenantAfterValidatedApartmentSharingNotValidated(TenantDto tenant);

    void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester);

}

