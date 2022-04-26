package fr.gouv.bo.service;

import com.google.common.base.Strings;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final String EMAIL = "Email";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private final MailjetClient client;

    @Value("${email.from}")
    private String emailFrom;
    @Value("${mailjet.template.id.message.notification}")
    private Integer templateIDMessageNotification;
    @Value("${mailjet.template.id.account.deleted}")
    private Integer templateIdAccountDeleted;
    @Value("${mailjet.template.id.dossier.validated}")
    private Integer templateIDDossierValidated;

    private void sendMailJetApi(String fromEmail, String fromName, String toEmail, String toName, String replyToEmail, String replyToName, String ccEmail, String ccName, String bccEmail, String bccName, String subject, Map<String, String> variables, int templateID) {
        MailjetRequest request;
        MailjetResponse response;

        JSONObject messagge = new JSONObject();
        //from
        if (fromEmail != null) {
            JSONObject from = new JSONObject();
            from.put(EMAIL, fromEmail);
            if (fromName != null) {
                from.put("Name", fromName);
            }
            messagge.put(Emailv31.Message.FROM, from);
        }

        //to
        if (toEmail != null) {
            JSONArray to = new JSONArray();
            JSONObject to1 = new JSONObject();
            to1.put(EMAIL, toEmail);
            if (toName != null) {
                to1.put("Name", toName);
            }
            messagge.put(Emailv31.Message.TO, to.put(to1));
        }

        //replyTo
        if (replyToEmail != null) {
            JSONObject replyTo = new JSONObject();
            replyTo.put(EMAIL, replyToEmail);
            if (replyToName != null) {
                replyTo.put("Name", replyToName);
            }
            messagge.put(Emailv31.Message.REPLYTO, replyTo);
        }

        //cc
        if (ccEmail != null) {
            JSONArray cc = new JSONArray();
            JSONObject cc1 = new JSONObject();
            cc1.put(EMAIL, ccEmail);
            if (ccName != null) {
                cc1.put("Name", ccEmail);
            }
            messagge.put(Emailv31.Message.CC, cc.put(cc1));
        }

        //bcc
        if (bccEmail != null) {
            JSONArray bcc = new JSONArray();
            JSONObject bcc1 = new JSONObject();
            bcc1.put(EMAIL, bccEmail);
            if (bccName != null) {
                bcc1.put("Name", bccEmail);
            }
            messagge.put(Emailv31.Message.BCC, bcc.put(bcc1));
        }

        //subject
        if (subject != null) {
            messagge.put(Emailv31.Message.SUBJECT, subject);
        }

        //template language
        messagge.put(Emailv31.Message.TEMPLATELANGUAGE, true);

        //variables
        if (variables != null) {
            messagge.put(Emailv31.Message.VARIABLES, new JSONObject(variables));
        }

        //templateID
        messagge.put(Emailv31.Message.TEMPLATEID, templateID);

        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(messagge));
        try {
            response = client.post(request);
            log.info("ResponseStatus: {}", response.getStatus());
            log.info("Response: {}", response.getData());
        } catch (MailjetException e) {
            log.error("MailjetException", e);
        }
    }

    @Async
    public void sendEmailAccountDeleted(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put(FIRST_NAME, user.getFirstName());
        variables.put(LAST_NAME, Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        sendMailJetApi(emailFrom, null, user.getEmail(), user.getFullName(), null, null, null, null, null, null, null, variables, templateIdAccountDeleted);
    }

    @Async
    public void sendMailNotificationAfterDeny(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put(FIRST_NAME, user.getFirstName());
        sendMailJetApi(emailFrom, null, user.getEmail(), user.getFullName(), null, null, null, null, null, null, null, variables, templateIDMessageNotification);
    }

    @Async
    public void sendEmailToTenantAfterValidateAllDocuments(Tenant tenant) {
        Map<String, String> variables = new HashMap<>();
        variables.put(FIRST_NAME, tenant.getFirstName());
        variables.put(LAST_NAME, Strings.isNullOrEmpty(tenant.getPreferredName()) ? tenant.getLastName() : tenant.getPreferredName());
        sendMailJetApi(emailFrom, null, tenant.getEmail(), tenant.getFullName(), null, null, null, null, null, null, null, variables, templateIDDossierValidated);
    }
}
