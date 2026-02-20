package fr.gouv.bo.service;

import com.google.common.base.Strings;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserApiDto;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.utils.OptionalString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    private static final String TENANT_BASE_URL_KEY = "tenantBaseUrl";
    private static final String TENANT_ID_KEY = "TENANT_ID";
    @Value("${tenant.base.url}")
    private String tenantBaseUrl;
    @Value("${brevo.template.id.message.notification:110}")
    private Long templateIDMessageNotification;
    @Value("${brevo.template.id.message.notification.with.details:155}")
    private Long templateIDMessageNotificationWithDetails;
    @Value("${brevo.template.id.account.deleted:39}")
    private Long templateIdAccountDeleted;
    @Value("${brevo.template.id.dossier.tenant.denied:121}")
    private Long templateIdDossierTenantDenied;
    @Value("${brevo.template.id.dossier.tenant.denied.with.details:158}")
    private Long templateIdDossierTenantDeniedWithDetails;
    @Value("${brevo.template.id.dossier.amount.changed:165}")
    private Long templateIdDossierAmountChanged;
    @Value("${brevo.template.id.message.notification.with.partner:126}")
    private Long templateIDMessageNotificationWithPartner;
    @Value("${brevo.template.id.message.notification.with.partner.and.details:156}")
    private Long templateIdMessageNotificationWithPartnerAndDetails;
    @Value("${brevo.template.id.dossier.tenant.denied.with.partner:124}")
    private Long templateIdDossierTenantDeniedWithPartner;
    @Value("${brevo.template.id.dossier.tenant.denied.with.partner.and.details:159}")
    private Long templateIdDossierTenantDeniedWithPartnerAndDetails;
    @Value("${link.after.denied.default}")
    private String defaultDeniedUrl;

    private final MailCommonService mailCommonService;


    @Async
    public void sendEmailAccountDeleted(TenantDto tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        mailCommonService.sendEmailToTenant(tenant, params, templateIdAccountDeleted);
    }

    @Async
    public void sendEmailAmountChanged(TenantDto tenant) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        mailCommonService.sendEmailToTenant(tenant, params, templateIdDossierAmountChanged);
    }

    @Async
    public void sendMailNotificationAfterDeny(TenantDto tenant, Message message) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        params.put(TENANT_ID_KEY, tenant.getId().toString());

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().getFirst();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getDeniedUrl()).orElse(defaultDeniedUrl));
            sendMailWithHtmlDetails(message, tenant, params, templateIdMessageNotificationWithPartnerAndDetails, templateIDMessageNotificationWithPartner);
        } else {
            sendMailWithHtmlDetails(message, tenant, params, templateIDMessageNotificationWithDetails, templateIDMessageNotification);
        }
    }

    private void sendMailWithHtmlDetails(Message message, TenantDto tenant, Map<String, String> params, Long templateWithHtml, Long templateWithoutHtml) {
        if (message != null) {
            params.put("HTML", OptionalString.of(message.getEmailHtml()).orElse(""));
            mailCommonService.sendEmailToTenant(tenant, params, templateWithHtml);
        } else {
            mailCommonService.sendEmailToTenant(tenant, params, templateWithoutHtml);
        }
    }

    @Async
    public void sendEmailToTenantAfterTenantDenied(TenantDto tenant, TenantDto deniedTenant, Message message) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", deniedTenant.getFirstName());
        params.put("NOM", Strings.isNullOrEmpty(deniedTenant.getPreferredName()) ? deniedTenant.getLastName() : deniedTenant.getPreferredName());
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        params.put(TENANT_ID_KEY, tenant.getId().toString());

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().getFirst();
            params.put("partnerName", userApi.getName2());
            params.put("logoUrl", userApi.getLogoUrl());
            params.put("callToActionUrl", OptionalString.of(userApi.getDeniedUrl()).orElse(defaultDeniedUrl));
            sendMailWithHtmlDetails(message, tenant, params, templateIdDossierTenantDeniedWithPartnerAndDetails, templateIdDossierTenantDeniedWithPartner);
        } else {
            sendMailWithHtmlDetails(message, tenant, params, templateIdDossierTenantDeniedWithDetails, templateIdDossierTenantDenied);
        }
    }
}
