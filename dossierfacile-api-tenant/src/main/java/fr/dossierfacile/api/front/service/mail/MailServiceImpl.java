package fr.dossierfacile.api.front.service.mail;

import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private final TransactionalEmailsApi apiInstance;
    private final SendinblueEmailTemplateIds emailTemplates;
    private final String sendinBlueUrlDomain;
    private final String emailSupport;

    public MailServiceImpl(TransactionalEmailsApi apiInstance,
                           SendinblueEmailTemplateIds emailTemplates,
                           @Value("${sendinblue.url.domain}") String sendinBlueUrlDomain,
                           @Value("${email.support}") String emailSupport) {
        this.apiInstance = apiInstance;
        this.emailTemplates = emailTemplates;
        this.sendinBlueUrlDomain = sendinBlueUrlDomain;
        this.emailSupport = emailSupport;
    }

    @Async
    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getWelcomeEmail())
                .withParam("confirmToken", confirmationToken.getToken())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getNewPasswordEmail())
                .withParam("newPasswordToken", passwordRecoveryToken.getToken())
                .withParam("PRENOM", user.getFirstName())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType) {
        Long templateId = applicationType == ApplicationType.GROUP
                ? emailTemplates.getGroupApplicationEmail()
                : emailTemplates.getCoupleApplicationEmail();

        EmailBuilder emailBuilder = EmailBuilder.fromTemplate(templateId);

        if (applicationType == ApplicationType.GROUP) {
            emailBuilder.withParam("NOM", flatmate.getPreferredLastName());
        }

        SendSmtpEmail email = emailBuilder
                .withParam("PRENOM", flatmate.getFirstName())
                .withParam("confirmToken", passwordRecoveryToken.getToken())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(guest).build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailAccountDeleted(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getAccountDeletedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailAccountCompleted(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getAccountCompletedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getAccountNotYetValidatedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("confirmToken", confirmationToken.getToken())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailWhenAccountNotYetCompleted(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getAccountNotYetCompletedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailWhenAccountIsStillDeclined(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getAccountIsStillDeclinedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Override
    public void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getTenantNotAssociatedToPartnersAndValidatedEmail())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Override
    public void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getTenantAssociatedToPartnersAndValidatedEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Override
    public void sendEmailFirstWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getFirstWarningBeforeDocumentsDeletionEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("confirmToken", confirmationToken.getToken())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Override
    public void sendEmailSecondWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getSecondWarningBeforeDocumentsDeletionEmail())
                .withParam("PRENOM", user.getFirstName())
                .withParam("NOM", user.getPreferredLastName())
                .withParam("confirmToken", confirmationToken.getToken())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    @Async
    @Override
    public void sendEmailToSupport(ContactForm form) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getContactSupportEmail())
                .withParam("firstname", form.getFirstname())
                .withParam("lastname", form.getLastname())
                .withParam("email", form.getEmail())
                .withParam("profile", form.getProfile())
                .withParam("subject", form.getSubject())
                .withParam("message", form.getMessage())
                .to(emailSupport, "Support depuis formulaire")
                .replyTo(form.getEmail())
                .build();

        send(email);
    }

    @Override
    public void sendEmailWelcomeForPartnerUser(User user, UserApi userApi) {
        SendSmtpEmail email = EmailBuilder.fromTemplate(emailTemplates.getWelcomePartnerEmail())
                .withParam("partnerName", userApi.getName2())
                .withParam("sendinBlueUrlDomain", sendinBlueUrlDomain)
                .to(user)
                .build();

        send(email);
    }

    private void send(SendSmtpEmail email) {
        try {
            apiInstance.sendTransacEmail(email);
        } catch (ApiException e) {
            log.error("Unable to send email to " + email.getTo(), e);
        }
    }
}
