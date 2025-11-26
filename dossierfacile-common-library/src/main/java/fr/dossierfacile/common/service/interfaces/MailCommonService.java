package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.dto.mail.TenantDto;

public interface MailCommonService {

    void sendEmailToTenantAfterValidateAllTenantForGroup(TenantDto tenant);

    void sendEmailToTenantAfterValidateAllDocuments(TenantDto tenant);

    void sendEmailToTenantAfterValidatedApartmentSharingNotValidated(TenantDto tenant);
}

