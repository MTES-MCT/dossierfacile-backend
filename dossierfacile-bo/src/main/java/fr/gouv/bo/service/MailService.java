package fr.gouv.bo.service;

import com.google.common.base.Strings;
import fr.dossierfacile.common.entity.OperationAccessToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.utils.OptionalString;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    private final TransactionalEmailsApi apiInstance;
    @Value("${sendinblue.url.domain}")
    private String sendinBlueUrlDomain;
    @Value("${application.domain}")
    private String applicationDomain;
    @Value("${sendinblue.template.id.message.notification}")
    private Long templateIDMessageNotification;
    @Value("${sendinblue.template.id.account.deleted}")
    private Long templateIdAccountDeleted;
    @Value("${sendinblue.template.id.dossier.validated}")
    private Long templateIdDossierValidated;
    @Value("${sendinblue.template.id.dossier.fully.validated}")
    private Long templateIdDossierFullyValidated;
    @Value("${sendinblue.template.id.dossier.tenant.denied}")
    private Long templateIdDossierTenantDenied;
    @Value("${sendinblue.template.id.message.notification.with.partner}")
    private Long templateIDMessageNotificationWithPartner;
    @Value("${sendinblue.template.id.dossier.validated.with.partner}")
    private Long templateIdDossierValidatedWithPartner;
    @Value("${sendinblue.template.id.dossier.fully.validated.with.partner}")
    private Long templateIdDossierFullyValidatedWithPartner;
    @Value("${sendinblue.template.id.dossier.tenant.denied.with.partner}")
    private Long templateIdDossierTenantDeniedWithPartner;
    @Value("${sendinblue.template.id.admin.partner.client.configuration}")
    private Long templateIdAdminPartnerConfiguration;
    @Value("${link.after.denied.default}")
    private String defaultDeniedUrl;
    @Value("${link.after.validated.default}")
    private String defaultValidatedUrl;

    private void sendEmailToTenant(User tenant, Map<String, String> params, Long templateId) {
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(tenant.getEmail());
        OptionalString.of(tenant.getFullName()).ifNotBlank( name -> sendSmtpEmailTo.setName(name));

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateId);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception" + Sentry.captureException(e), e);
        }
    }

    @Async
    public void sendEmailAccountDeleted(Tenant tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        sendEmailToTenant(tenant, params, templateIdAccountDeleted);
    }

    @Async
    public void sendMailNotificationAfterDeny(Tenant tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        if (tenant.isBelongToPartner()) {
            UserApi userApi = tenant.getTenantsUserApi().get(0).getUserApi();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getDeniedUrl()).orElse(defaultDeniedUrl));

            sendEmailToTenant(tenant, params, templateIDMessageNotificationWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIDMessageNotification);
        }
    }

    @Async
    public void sendEmailToTenantAfterValidateAllDocumentsOfTenant(Tenant tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        if (tenant.isBelongToPartner()) {
            UserApi userApi = tenant.getTenantsUserApi().get(0).getUserApi();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getValidatedUrl()).orElse(defaultValidatedUrl));

            sendEmailToTenant(tenant, params, templateIdDossierValidatedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdDossierValidated);
        }
    }

    @Async
    public void sendEmailToTenantAfterValidateAllDocuments(Tenant tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        if (tenant.isBelongToPartner()) {
            UserApi userApi = tenant.getTenantsUserApi().get(0).getUserApi();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getValidatedUrl()).orElse(defaultValidatedUrl));

            sendEmailToTenant(tenant, params, templateIdDossierFullyValidatedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdDossierFullyValidated);
        }
    }

    @Async
    public void sendEmailToTenantAfterTenantDenied(Tenant tenant, Tenant deniedTenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", deniedTenant.getFirstName());
        params.put("NOM", Strings.isNullOrEmpty(deniedTenant.getPreferredName()) ? deniedTenant.getLastName() : deniedTenant.getPreferredName());
        params.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        if (tenant.isBelongToPartner()) {
            UserApi userApi = tenant.getTenantsUserApi().get(0).getUserApi();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getDeniedUrl()).orElse(defaultDeniedUrl));

            sendEmailToTenant(tenant, params, templateIdDossierTenantDeniedWithPartner);
        } else {
            sendEmailToTenant(tenant, params, templateIdDossierTenantDenied);
        }
    }

    public void sendClientConfiguration(UserApi userApi, ClientRepresentation client, String email, String token) {
        Map<String, String> params = new HashMap<>();
        params.put("clientName", userApi.getName());
        params.put("secretUrl",  applicationDomain + "/api/onetimesecret/" + token);
        params.put("redirectUrls", String.join(", ", client.getRedirectUris()));
        params.put("webhookUrl", userApi.getUrlCallback());
        params.put("apiKey", userApi.getPartnerApiKeyCallback());

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdAdminPartnerConfiguration);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(List.of(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception" + Sentry.captureException(e), e);
        }
    }
}
