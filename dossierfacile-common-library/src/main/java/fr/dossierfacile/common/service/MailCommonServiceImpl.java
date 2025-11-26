package fr.dossierfacile.common.service;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailTo;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserApiDto;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.utils.OptionalString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import fr.dossierfacile.common.dto.mail.UserDto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailCommonServiceImpl implements MailCommonService {

    private static final String TENANT_BASE_URL_KEY = "tenantBaseUrl";
    private final TransactionalEmailsApi apiInstance;
    @Value("${tenant.base.url}")
    private String tenantBaseUrl;
    @Value("${brevo.template.id.tenant.validated.dossier.validated}")
    private Long templateIdTenantValidatedDossierValidated;
    @Value("${brevo.template.id.tenant.validated.dossier.validated.w.partner}")
    private Long templateIdTenantValidatedDossierValidatedWithPartner;
    @Value("${brevo.template.id.tenant.validated.dossier.not.valid}")
    private Long templateIdTenantValidatedDossierNotValidated;
    @Value("${brevo.template.id.tenant.validated.dossier.not.valid.w.partner}")
    private Long templateIdTenantValidatedDossierNotValidatedWithPartner;
    @Value("${brevo.template.id.dossier.fully.validated}")
    private Long templateIdDossierFullyValidated;
    @Value("${brevo.template.id.dossier.fully.validated.with.partner}")
    private Long templateIdDossierFullyValidatedWithPartner;
    @Value("${link.after.validated.default}")
    private String defaultValidatedUrl;

    private void sendEmailToTenant(UserDto tenant, Map<String, String> params, Long templateId) {
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(tenant.getEmail());
        OptionalString.of(tenant.getFullName()).ifNotBlank(name -> sendSmtpEmailTo.setName(name));

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateId);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidateAllTenantForGroup(TenantDto tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        params.put("TENANT_ID", tenant.getId().toString());

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().getFirst();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getValidatedUrl()).orElse(defaultValidatedUrl));

            sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierValidatedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierValidated);
        }
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidatedApartmentSharingNotValidated(TenantDto tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().getFirst();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getValidatedUrl()).orElse(defaultValidatedUrl));

            sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierNotValidatedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierNotValidated);
        }
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidateAllDocuments(TenantDto tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        params.put("TENANT_ID", tenant.getId().toString());

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().getFirst();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getValidatedUrl()).orElse(defaultValidatedUrl));

            sendEmailToTenant(tenant, params, templateIdDossierFullyValidatedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdDossierFullyValidated);
        }
    }
    

}

