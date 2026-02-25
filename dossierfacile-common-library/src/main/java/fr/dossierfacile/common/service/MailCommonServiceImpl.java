package fr.dossierfacile.common.service;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailTo;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.utils.OptionalString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.dossierfacile.common.enums.ApplicationType.COUPLE;

@RequiredArgsConstructor
@Slf4j
public class MailCommonServiceImpl implements MailCommonService {

    private static final String TENANT_BASE_URL_KEY = "tenantBaseUrl";
    private final TransactionalEmailsApi apiInstance;
    @Value("${tenant.base.url}")
    private String tenantBaseUrl;
    @Value("${brevo.template.id.tenant.validated.dossier.validated:134}")
    private Long templateIdTenantValidatedDossierValidated;
    @Value("${brevo.template.id.tenant.validated.dossier.not.valid:122}")
    private Long templateIdTenantValidatedDossierNotValidated;
    @Value("${brevo.template.id.dossier.fully.validated:128}")
    private Long templateIdDossierFullyValidated;
    @Value("${brevo.template.id.partner.access.revoked:104}")
    private Long templateIDPartnerAccessRevoked;

    @Override
    public void sendEmailToTenant(UserDto tenant, Map<String, String> params, Long templateId) {
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(tenant.getEmail());
        OptionalString.of(tenant.getFullName()).ifNotBlank(sendSmtpEmailTo::setName);

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

    private Map<String, String> createBaseParams(TenantDto tenant, boolean includeTenantId) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", tenant.getFirstName());
        params.put("NOM", OptionalString.of(tenant.getPreferredName()).orElse(tenant.getLastName()));
        params.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        if (includeTenantId) {
            params.put("TENANT_ID", tenant.getId().toString());
        }

        return params;
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidateAllTenantForGroup(TenantDto tenant) {
        Map<String, String> params = createBaseParams(tenant, true);
        sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierValidated);
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidatedApartmentSharingNotValidated(TenantDto tenant) {
        Map<String, String> params = createBaseParams(tenant, false);
        sendEmailToTenant(tenant, params, templateIdTenantValidatedDossierNotValidated);
    }

    @Async
    @Override
    public void sendEmailToTenantAfterValidateAllDocuments(TenantDto tenant) {
        Map<String, String> params = createBaseParams(tenant, true);
        sendEmailToTenant(tenant, params, templateIdDossierFullyValidated);
    }

    @Override
    public void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester) {
        Map<String, String> params = new HashMap<>();
        params.put("PRENOM", receiver.getFirstName());
        params.put("NOM", OptionalString.of(receiver.getPreferredName()).orElse(receiver.getLastName()));
        params.put("partnerName", userApi.getName2());
        params.put("requestOrigin", new RevocationRequest(revocationRequester, receiver).getOrigin());

        UserDto receiverDto = new UserDto(
                receiver.getFirstName(),
                receiver.getLastName(),
                receiver.getPreferredName(),
                receiver.getEmail(),
                receiver.getKeycloakId()
        );
        sendEmailToTenant(receiverDto, params, templateIDPartnerAccessRevoked);
    }

    private record RevocationRequest(Tenant requester, Tenant emailReceiver) {

        String getOrigin() {
            if (requester.getId().equals(emailReceiver.getId())) {
                return "votre demande";
            }
            ApplicationType applicationType = requester.getApartmentSharing().getApplicationType();
            String requesterType = applicationType == COUPLE ? "conjoint(e)" : "colocataire";
            return String.format("la demande de votre %s %s", requesterType, requester.getFirstName());
        }

    }

}
