package fr.dossierfacile.api.front.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import fr.dossierfacile.api.front.model.MailJetModel;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.mailjet.client.resource.Emailv31.Message.EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private final MailjetClient client;
    @Value("${email.from}")
    private String emailFrom;
    @Value("${mailjet.template.id.welcome}")
    private Integer templateIDWelcome;
    @Value("${mailjet.template.id.new.password}")
    private Integer templateIdNewPassword;

    @Value("${mailjet.template.id.invitation.couple}")
    private Integer templateIdCoupleApplication;
    @Value("${mailjet.template.id.invitation.group}")
    private Integer templateIdGroupApplication;
    @Value("${mailjet.template.id.account.deleted}")
    private Integer templateIdAccountDeleted;
    @Value("${mailjet.template.id.account.completed}")
    private Integer templateIdAccountCompleted;
    @Value("${mailjet.template.id.account.email_validation_reminder}")
    private Integer templateEmailWhenEmailAccountNotYetValidated;
    @Value("${mailjet.template.id.account.incomplete_reminder}")
    private Integer templateEmailWhenAccountNotYetCompleted;
    @Value("${mailjet.template.id.account.declined_reminder}")
    private Integer templateEmailWhenAccountIsStillDeclined;
    @Value("${mailjet.template.id.account.satisfaction_email_to_tenants_NOT_associated_to_partners}")
    private Integer templateEmailWhenTenantNOTAssociatedToPartnersAndValidated;
    @Value("${mailjet.template.id.account.satisfaction_email_to_tenants_YES_associated_to_partners}")
    private Integer templateEmailWhenTenantYESAssociatedToPartnersAndValidated;

    @Async
    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("confirmToken", confirmationToken.getToken());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .variables(variables)
                .templateID(templateIDWelcome)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("createPasswordToken", passwordRecoveryToken.getToken());
        variables.put("firstName", user.getFirstName());

        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .variables(variables)
                .templateID(templateIdNewPassword)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType) {
        Map<String, String> variables = new HashMap<>();

        Integer templateId = templateIdCoupleApplication;
        variables.put("firstName", flatmate.getFirstName());
        variables.put("password_recovery_url", passwordRecoveryToken.getToken());
        if (applicationType == ApplicationType.GROUP) {
            variables.put("lastName", flatmate.getLastName());
            templateId = templateIdGroupApplication;
        }

        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(guest.getEmail())
                .variables(variables)
                .templateID(templateId)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailAccountDeleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .variables(variables)
                .templateID(templateIdAccountDeleted)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailAccountCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("FIRSTNAME_TENANT", user.getFirstName());
        variables.put("LASTNAME_TENANT", user.getLastName());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .variables(variables)
                .templateID(templateIdAccountCompleted)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        variables.put("confirmToken", confirmationToken.getToken());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .variables(variables)
                .templateID(templateEmailWhenEmailAccountNotYetValidated)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailWhenAccountNotYetCompleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .variables(variables)
                .templateID(templateEmailWhenAccountNotYetCompleted)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Async
    @Override
    public void sendEmailWhenAccountIsStillDeclined(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .variables(variables)
                .templateID(templateEmailWhenAccountIsStillDeclined)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Override
    public void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .templateID(templateEmailWhenTenantNOTAssociatedToPartnersAndValidated)
                .build();
        sendMailJetApi(mailjetModel);
    }

    @Override
    public void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user) {
        MailJetModel mailjetModel = MailJetModel.builder()
                .fromEmail(emailFrom)
                .toEmail(user.getEmail())
                .toName(user.getFullName())
                .templateID(templateEmailWhenTenantYESAssociatedToPartnersAndValidated)
                .build();
        sendMailJetApi(mailjetModel);
    }

    private void sendMailJetApi(MailJetModel mailjetModel) {
        MailjetRequest request;
        MailjetResponse response;

        JSONObject message = new JSONObject();
        //from
        if (mailjetModel.getFromEmail() != null) {
            JSONObject from = new JSONObject();
            from.put(EMAIL, mailjetModel.getFromEmail());
            if (mailjetModel.getFromName() != null) {
                from.put("Name", mailjetModel.getFromName());
            }
            message.put(Emailv31.Message.FROM, from);
        }

        //to
        if (mailjetModel.getToEmail() != null) {
            JSONArray to = new JSONArray();
            JSONObject to1 = new JSONObject();
            to1.put(EMAIL, mailjetModel.getToEmail());
            if (mailjetModel.getToName() != null) {
                to1.put("Name", mailjetModel.getToName());
            }
            message.put(Emailv31.Message.TO, to.put(to1));
        }

        //replyTo
        if (mailjetModel.getReplyToEmail() != null) {
            JSONObject replyTo = new JSONObject();
            replyTo.put(EMAIL, mailjetModel.getReplyToEmail());
            if (mailjetModel.getReplyToName() != null) {
                replyTo.put("Name", mailjetModel.getReplyToName());
            }
            message.put(Emailv31.Message.REPLYTO, replyTo);
        }

        //cc
        if (mailjetModel.getCcEmail() != null) {
            JSONArray cc = new JSONArray();
            JSONObject cc1 = new JSONObject();
            cc1.put(EMAIL, mailjetModel.getCcEmail());
            if (mailjetModel.getCcName() != null) {
                cc1.put("Name", mailjetModel.getCcName());
            }
            message.put(Emailv31.Message.CC, cc.put(cc1));
        }

        //bcc
        if (mailjetModel.getBccEmail() != null) {
            JSONArray bcc = new JSONArray();
            JSONObject bcc1 = new JSONObject();
            bcc1.put(EMAIL, mailjetModel.getBccEmail());
            if (mailjetModel.getCcName() != null) {
                bcc1.put("Name", mailjetModel.getCcName());
            }
            message.put(Emailv31.Message.BCC, bcc.put(bcc1));
        }

        //subject
        if (mailjetModel.getSubject() != null) {
            message.put(Emailv31.Message.SUBJECT, mailjetModel.getSubject());
        }

        //template language
        message.put(Emailv31.Message.TEMPLATELANGUAGE, true);

        //variables
        if (mailjetModel.getVariables() != null) {
            message.put(Emailv31.Message.VARIABLES, new JSONObject(mailjetModel.getVariables()));
        }

        //templateID
        message.put(Emailv31.Message.TEMPLATEID, mailjetModel.getTemplateID());

        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(message));
        try {
            response = client.post(request);
            log.info("ResponseStatus: {}", response.getStatus());
            log.info("Response: {}", response.getData());
        } catch (MailjetException e) {
            log.error("MailjetException", e);
        }
    }
}
