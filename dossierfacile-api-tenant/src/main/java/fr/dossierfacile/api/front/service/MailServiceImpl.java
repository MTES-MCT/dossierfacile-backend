package fr.dossierfacile.api.front.service;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailReplyTo;
import brevoModel.SendSmtpEmailTo;
import com.google.common.base.Strings;
import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserApiDto;
import fr.dossierfacile.common.dto.mail.UserDto;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.utils.OptionalString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static fr.dossierfacile.common.enums.ApplicationType.COUPLE;
import static fr.dossierfacile.common.enums.ApplicationType.GROUP;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private static final String TENANT_BASE_URL_KEY = "tenantBaseUrl";
    private static final String TENANT_ID_KEY = "TENANT_ID";
    private final TransactionalEmailsApi apiInstance;
    @Value("${tenant.base.url}")
    private String tenantBaseUrl;
    @Value("${email.support}")
    private String emailSupport;
    @Value("${email.support.owner}")
    private String emailSupportOwner;
    @Value("${brevo.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${brevo.template.id.welcome.partner}")
    private Long templateIDWelcomePartner;
    @Value("${brevo.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${brevo.template.id.invitation.couple}")
    private Long templateIdCoupleApplication;
    @Value("${brevo.template.id.invitation.group}")
    private Long templateIdGroupApplication;
    @Value("${brevo.template.id.account.deleted}")
    private Long templateIdAccountDeleted;
    @Value("${brevo.template.id.account.completed}")
    private Long templateIdAccountCompleted;
    @Value("${brevo.template.id.account.completed.with.partner}")
    private Long templateIdAccountCompletedWithPartner;
    @Value("${brevo.template.id.account.email.validation.reminder}")
    private Long templateEmailWhenEmailAccountNotYetValidated;
    @Value("${brevo.template.id.account.incomplete.reminder}")
    private Long templateEmailWhenAccountNotYetCompleted;
    @Value("${brevo.template.id.account.declined.reminder}")
    private Long templateEmailWhenAccountIsStillDeclined;
    @Value("${brevo.template.id.account.satisf}")
    private Long templateIdAccountSatisf;
    @Value("${brevo.template.id.contact.support}")
    private Long templateIdContactSupport;
    @Value("${brevo.template.id.token.expiration}")
    private Long templateIdTokenExpiration;
    @Value("${brevo.template.id.share.file}")
    private Long templateIdShareFile;
    @Value("${brevo.template.id.partner.access.revoked}")
    private Long templateIDPartnerAccessRevoked;
    @Value("${link.after.completed.default}")
    private String defaultCompletedUrl;
    @Value("${link.after.created.default}")
    private String defaultCreatedUrl;
    @Value("${brevo.domains.blacklist:example.com}")
    private List<String> blackListedDomains;

    private void sendEmailToTenant(String email, String tenantFullName, Map<String, String> params, Long templateId) {
        if (email == null) {
            return;
        }
        boolean blackListed = blackListedDomains.stream().anyMatch(email::endsWith);
        if (blackListed) {
            return;
        }
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);
        if (!Strings.isNullOrEmpty(tenantFullName)) {
            sendSmtpEmailTo.setName(tenantFullName);
        }
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateId);
        if (params.values().stream().anyMatch(Objects::nonNull)) {
            sendSmtpEmail.params(params);
        }
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception", e);
        }
    }

    private void sendEmailToTenant(User tenant, Map<String, String> params, Long templateId) {
        sendEmailToTenant(tenant.getEmail(), tenant.getFullName(), params, templateId);
    }

    private void sendEmailToTenant(UserDto tenant, Map<String, String> params, Long templateId) {
        sendEmailToTenant(tenant.getEmail(), tenant.getFullName(), params, templateId);
    }

    @Override
    public void sendEmailConfirmAccount(UserDto user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        sendEmailToTenant(user, variables, templateIDWelcome);
    }

    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("newPasswordToken", passwordRecoveryToken.getToken());
        variables.put("PRENOM", user.getFirstName());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        sendEmailToTenant(user, variables, templateIdNewPassword);
    }

    @Override
    public void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType) {
        Map<String, String> variables = new HashMap<>();

        Long templateId = templateIdCoupleApplication;
        variables.put("PRENOM", flatmate.getFirstName());
        variables.put(TENANT_ID_KEY, flatmate.getId().toString());
        variables.put("confirmToken", passwordRecoveryToken.getToken());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        if (applicationType == GROUP) {
            variables.put("NOM", Strings.isNullOrEmpty(flatmate.getPreferredName()) ? flatmate.getLastName() : flatmate.getPreferredName());
            templateId = templateIdGroupApplication;
        }

        sendEmailToTenant(guest, variables, templateId);
    }

    @Async
    @Override
    public void sendEmailAccountDeleted(UserDto user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());

        sendEmailToTenant(user, variables, templateIdAccountDeleted);
    }

    @Async
    @Override
    public void sendEmailAccountCompleted(TenantDto tenant) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", tenant.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(tenant.getPreferredName()) ? tenant.getLastName() : tenant.getPreferredName());

        if (tenant.isBelongToPartner()) {
            UserApiDto userApi = tenant.getUserApis().get(0);
            variables.put("partnerName", userApi.getName2());
            variables.put("logoUrl", userApi.getLogoUrl());
            variables.put("callToActionUrl", OptionalString.of(userApi.getCompletedUrl()).orElse(defaultCompletedUrl));

            sendEmailToTenant(tenant, variables, templateIdAccountCompletedWithPartner);
        } else {
            variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
            sendEmailToTenant(tenant, variables, templateIdAccountCompleted);
        }
    }

    @Override
    public void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        sendEmailToTenant(user, variables, templateEmailWhenEmailAccountNotYetValidated);
    }

    @Override
    public void sendEmailWhenAccountNotYetCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put(TENANT_ID_KEY, user.getId().toString());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        sendEmailToTenant(user, variables, templateEmailWhenAccountNotYetCompleted);
    }

    @Override
    public void sendEmailWhenAccountIsStillDeclined(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);

        sendEmailToTenant(user, variables, templateEmailWhenAccountIsStillDeclined);
    }

    @Override
    public void sendEmailToTenantValidatedXDaysAgo(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        variables.put(TENANT_ID_KEY, user.getId().toString());

        sendEmailToTenant(user, variables, templateIdAccountSatisf);
    }

    @Override
    public void sendEmailToSupport(ContactForm form) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstname", form.getFirstname());
        variables.put("lastname", form.getLastname());
        variables.put("email", form.getEmail());
        if (form.getProfile() != null) {
            variables.put("profile", form.getProfile().name());
        }
        variables.put("subject", form.getSubject());
        variables.put("message", form.getMessage());

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();

        String recipientEmail = (ContactForm.Profile.owner == form.getProfile()) ? emailSupportOwner : emailSupport;
        sendSmtpEmailTo.setEmail(recipientEmail);
        sendSmtpEmailTo.setName("Support depuis formulaire");

        SendSmtpEmailReplyTo sendSmtpEmailReplyTo = new SendSmtpEmailReplyTo();
        sendSmtpEmailReplyTo.setEmail(form.getEmail());

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdContactSupport);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));
        sendSmtpEmail.replyTo(sendSmtpEmailReplyTo);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailWelcomeForPartnerUser(UserDto user, UserApiDto userApi) {
        Map<String, String> variables = new HashMap<>();
        variables.put("partnerName", userApi.getName2());
        variables.put("logoUrl", userApi.getLogoUrl());
        variables.put("callToActionUrl", OptionalString.of(userApi.getWelcomeUrl()).orElse(defaultCreatedUrl));

        sendEmailToTenant(user, variables, templateIDWelcomePartner);
    }

    @Override
    public void sendFileByMail(String url, String email, String tenantName, String fullName, String tenantEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("url", tenantBaseUrl + url);
        variables.put("tenantName", tenantName);
        variables.put("tenantEmail", tenantEmail);
        variables.put("fullName", fullName);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);
        SendSmtpEmailReplyTo sendSmtpEmailReplyTo = new SendSmtpEmailReplyTo();
        sendSmtpEmailReplyTo.setEmail(tenantEmail);

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdShareFile);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.replyTo(sendSmtpEmailReplyTo);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Error with brevo send mail – code={} , headers={} , body={}, message={}",
                    e.getCode(),
                    e.getResponseHeaders(),
                    e.getResponseBody(),
                    e.getMessage(),
                    e);
            throw new InternalError("Mail cannot be send - try later");
        }
    }

    @Override
    public void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", receiver.getFirstName());
        variables.put("partnerName", userApi.getName2());
        variables.put("requestOrigin", new RevocationRequest(revocationRequester, receiver).getOrigin());

        sendEmailToTenant(receiver, variables, templateIDPartnerAccessRevoked);
    }

    @Override
    public void sendDefaultEmailExpiredToken(String email, OperationAccessToken token) {
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(email);

        Map<String, String> params = new HashMap<>();
        params.put("operation", token.getOperationAccessType().name());
        params.put("createdDate", token.getCreatedDate().format(DateTimeFormatter.ISO_DATE));

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdTokenExpiration);
        sendSmtpEmail.params(params);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email Api Exception", e);
        }
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
