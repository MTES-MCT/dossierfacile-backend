package fr.dossierfacile.api.front.service.mail;

import fr.dossierfacile.api.front.extensions.RandomUuidResolver;
import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;

import java.util.UUID;

import static fr.dossierfacile.api.front.assertions.TenantAssertions.assertThat;
import static fr.dossierfacile.common.enums.UserType.TENANT;
import static org.mockito.Mockito.*;

@ExtendWith(RandomUuidResolver.class)
class MailServiceImplTest {

    private static final String DOMAIN = "locataire.dossierfacile.fr";

    private static final User JOHN = new User(TENANT, "John", "Doe", "john@doe.io");
    private static final User JANE = new User(TENANT, "Jane", "Doe", "jane@doe.io");

    private MailService mailService;
    private TransactionalEmailsApi emailsApi;
    private SendinblueEmailTemplateIds emailTemplates;

    @BeforeEach
    void setUp() {
        emailsApi = mock(TransactionalEmailsApi.class);
        emailTemplates = mock(SendinblueEmailTemplateIds.class);
        mailService = new MailServiceImpl(emailsApi, emailTemplates, DOMAIN, "support@dossierfacile.fr");
    }

    @Test
    void should_send_confirm_account_email(UUID token) throws ApiException {
        when(emailTemplates.getWelcomeEmail()).thenReturn(1L);

        mailService.sendEmailConfirmAccount(JOHN, buildConfirmationToken(token));

        assertThat(interceptSentEmail())
                .hasTemplateId(1L)
                .hasParameter("confirmToken", token.toString())
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_new_password_email(UUID token) throws ApiException {
        when(emailTemplates.getNewPasswordEmail()).thenReturn(2L);

        mailService.sendEmailNewPassword(JOHN, buildPasswordRecoveryToken(token));

        assertThat(interceptSentEmail())
                .hasTemplateId(2L)
                .hasParameter("newPasswordToken", token.toString())
                .hasParameter("PRENOM", "John")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_email_to_flatmates_for_couple_application(UUID token) throws ApiException {
        when(emailTemplates.getCoupleApplicationEmail()).thenReturn(3L);

        mailService.sendEmailForFlatmates(JOHN, JANE, buildPasswordRecoveryToken(token), ApplicationType.COUPLE);

        assertThat(interceptSentEmail())
                .hasTemplateId(3L)
                .hasParameter("confirmToken", token.toString())
                .hasParameter("PRENOM", "John")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("jane@doe.io", "Jane Doe");
    }

    @Test
    void should_send_email_to_flatmates_for_group_application(UUID token) throws ApiException {
        when(emailTemplates.getGroupApplicationEmail()).thenReturn(4L);

        mailService.sendEmailForFlatmates(JOHN, JANE, buildPasswordRecoveryToken(token), ApplicationType.GROUP);

        assertThat(interceptSentEmail())
                .hasTemplateId(4L)
                .hasParameter("confirmToken", token.toString())
                .hasParameter("PRENOM", "John")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasParameter("NOM", "Doe")
                .hasReceiver("jane@doe.io", "Jane Doe");
    }

    @Test
    void should_send_account_deleted_email() throws ApiException {
        when(emailTemplates.getAccountDeletedEmail()).thenReturn(5L);

        mailService.sendEmailAccountDeleted(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(5L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_account_completed_email() throws ApiException {
        when(emailTemplates.getAccountCompletedEmail()).thenReturn(6L);

        mailService.sendEmailAccountCompleted(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(6L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_account_validation_reminder_email(UUID token) throws ApiException {
        when(emailTemplates.getAccountNotYetValidatedEmail()).thenReturn(7L);

        mailService.sendEmailWhenEmailAccountNotYetValidated(JOHN, buildConfirmationToken(token));

        assertThat(interceptSentEmail())
                .hasTemplateId(7L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("confirmToken", token.toString())
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_application_not_completed_reminder_email() throws ApiException {
        when(emailTemplates.getAccountNotYetCompletedEmail()).thenReturn(8L);

        mailService.sendEmailWhenAccountNotYetCompleted(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(8L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_account_still_declined_email() throws ApiException {
        when(emailTemplates.getAccountIsStillDeclinedEmail()).thenReturn(9L);

        mailService.sendEmailWhenAccountIsStillDeclined(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(9L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_survey_after_validation_for_DF_users() throws ApiException {
        when(emailTemplates.getTenantNotAssociatedToPartnersAndValidatedEmail()).thenReturn(10L);

        mailService.sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(10L)
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_survey_after_validation_for_partner_users() throws ApiException {
        when(emailTemplates.getTenantAssociatedToPartnersAndValidatedEmail()).thenReturn(11L);

        mailService.sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(JOHN);

        assertThat(interceptSentEmail())
                .hasTemplateId(11L)
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_first_warning_before_documents_deletion(UUID token) throws ApiException {
        when(emailTemplates.getFirstWarningBeforeDocumentsDeletionEmail()).thenReturn(12L);

        mailService.sendEmailFirstWarningForDeletionOfDocuments(JOHN, buildConfirmationToken(token));

        assertThat(interceptSentEmail())
                .hasTemplateId(12L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("confirmToken", token.toString())
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_second_warning_before_documents_deletion(UUID token) throws ApiException {
        when(emailTemplates.getSecondWarningBeforeDocumentsDeletionEmail()).thenReturn(13L);

        mailService.sendEmailSecondWarningForDeletionOfDocuments(JOHN, buildConfirmationToken(token));

        assertThat(interceptSentEmail())
                .hasTemplateId(13L)
                .hasParameter("PRENOM", "John")
                .hasParameter("NOM", "Doe")
                .hasParameter("confirmToken", token.toString())
                .hasParameter("sendinBlueUrlDomain", DOMAIN)
                .hasReceiver("john@doe.io", "John Doe");
    }

    @Test
    void should_send_email_to_support() throws ApiException {
        when(emailTemplates.getContactSupportEmail()).thenReturn(14L);
        ContactForm form = new ContactForm("John", "Doe", "john@doe.io", "tenant", "Email subject", "Hello world");

        mailService.sendEmailToSupport(form);

        assertThat(interceptSentEmail())
                .hasTemplateId(14L)
                .hasParameter("firstname", "John")
                .hasParameter("lastname", "Doe")
                .hasParameter("email", "john@doe.io")
                .hasParameter("profile", "tenant")
                .hasParameter("subject", "Email subject")
                .hasParameter("message", "Hello world")
                .hasReplyTo("john@doe.io")
                .hasReceiver("support@dossierfacile.fr", "Support depuis formulaire");
    }

    private ConfirmationToken buildConfirmationToken(UUID token) {
        ConfirmationToken confirmationToken = new ConfirmationToken();
        confirmationToken.setToken(token.toString());
        return confirmationToken;
    }

    private PasswordRecoveryToken buildPasswordRecoveryToken(UUID token) {
        PasswordRecoveryToken passwordRecoveryToken = new PasswordRecoveryToken();
        passwordRecoveryToken.setToken(token.toString());
        return passwordRecoveryToken;
    }

    private SendSmtpEmail interceptSentEmail() throws ApiException {
        ArgumentCaptor<SendSmtpEmail> captor = ArgumentCaptor.forClass(SendSmtpEmail.class);
        verify(emailsApi).sendTransacEmail(captor.capture());
        return captor.getValue();
    }

    private static UserApi buildPartner(String name) {
        UserApi userApi = new UserApi();
        userApi.setName2(name);
        return userApi;
    }

}