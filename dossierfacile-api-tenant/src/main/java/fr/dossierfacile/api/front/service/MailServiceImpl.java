package fr.dossierfacile.api.front.service;

import com.google.common.base.Strings;
import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailReplyTo;
import sibModel.SendSmtpEmailTo;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private final TransactionalEmailsApi apiInstance;
    @Value("${sendinblue.url.domain}")
    private String sendinBlueUrlDomain;
    @Value("${email.support}")
    private String emailSupport;
    @Value("${sendinblue.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${sendinblue.template.id.welcome.partner}")
    private Long templateIDWelcomePartner;
    @Value("${sendinblue.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${sendinblue.template.id.invitation.couple}")
    private Long templateIdCoupleApplication;
    @Value("${sendinblue.template.id.invitation.group}")
    private Long templateIdGroupApplication;
    @Value("${sendinblue.template.id.account.deleted}")
    private Long templateIdAccountDeleted;
    @Value("${sendinblue.template.id.account.completed}")
    private Long templateIdAccountCompleted;
    @Value("${sendinblue.template.id.account.email.validation.reminder}")
    private Long templateEmailWhenEmailAccountNotYetValidated;
    @Value("${sendinblue.template.id.account.incomplete.reminder}")
    private Long templateEmailWhenAccountNotYetCompleted;
    @Value("${sendinblue.template.id.account.declined.reminder}")
    private Long templateEmailWhenAccountIsStillDeclined;
    @Value("${sendinblue.template.id.account.satisf.not.assoc.to.partners}")
    private Long templateEmailWhenTenantNOTAssociatedToPartnersAndValidated;
    @Value("${sendinblue.template.id.account.satisf.yes.assoc.to.partners}")
    private Long templateEmailWhenTenantYESAssociatedToPartnersAndValidated;
    @Value("${sendinblue.template.id.first.warning.for.deletion.of.documents}")
    private Long templateFirstWarningForDeletionOfDocuments;
    @Value("${sendinblue.template.id.second.warning.for.deletion.of.documents}")
    private Long templateSecondWarningForDeletionOfDocuments;
    @Value("${sendinblue.template.id.contact.support}")
    private Long templateIdContactSupport;

    @Async
    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        sendSmtpEmail.templateId(templateIDWelcome);
        sendSmtpEmail.params(variables);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("newPasswordToken", passwordRecoveryToken.getToken());
        variables.put("PRENOM", user.getFirstName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdNewPassword);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType) {
        Map<String, String> variables = new HashMap<>();

        Long templateId = templateIdCoupleApplication;
        variables.put("PRENOM", flatmate.getFirstName());
        variables.put("confirmToken", passwordRecoveryToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);
        if (applicationType == ApplicationType.GROUP) {
            variables.put("NOM", Strings.isNullOrEmpty(flatmate.getPreferredName()) ? flatmate.getLastName() : flatmate.getPreferredName());
            templateId = templateIdGroupApplication;
        }

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(guest.getEmail());
        if (!Strings.isNullOrEmpty(guest.getFullName())) {
            sendSmtpEmailTo.setName(guest.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateId);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailAccountDeleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdAccountDeleted);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailAccountCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIdAccountCompleted);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateEmailWhenEmailAccountNotYetValidated);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailWhenAccountNotYetCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateEmailWhenAccountNotYetCompleted);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailWhenAccountIsStillDeclined(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateEmailWhenAccountIsStillDeclined);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Override
    public void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateEmailWhenTenantNOTAssociatedToPartnersAndValidated);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Override
    public void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateEmailWhenTenantYESAssociatedToPartnersAndValidated);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Override
    public void sendEmailFirstWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateFirstWarningForDeletionOfDocuments);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Override
    public void sendEmailSecondWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateSecondWarningForDeletionOfDocuments);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailToSupport(ContactForm form) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstname", form.getFirstname());
        variables.put("lastname", form.getLastname());
        variables.put("email", form.getEmail());
        variables.put("profile", form.getProfile());
        variables.put("subject", form.getSubject());
        variables.put("message", form.getMessage());

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(emailSupport);
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
    public void sendEmailWelcomeForPartnerUser(User user, UserApi userApi) {
        Map<String, String> variables = new HashMap<>();
        variables.put("partnerName", userApi.getName2());
        variables.put("sendinBlueUrlDomain", sendinBlueUrlDomain);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateIDWelcomePartner);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }
}
