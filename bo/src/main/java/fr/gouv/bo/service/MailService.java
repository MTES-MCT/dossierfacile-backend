package fr.gouv.bo.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import fr.dossierfacile.common.entity.User;
import fr.gouv.bo.model.MailJetModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final String EMAIL = "Email";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ADRESSE_BETATESTEUR = "ADRESSE_BETATESTEUR";

    private final MailjetClient client;
    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String emailFrom;
    @Value("${mailjet.template.id.message.notification}")
    private Integer templateIDMessageNotification;
    @Value("${email.from.admin1}")
    private String emailAdmin1;
    @Value("${email.from.admin1}")
    private String emailAdmin2;
    @Value("${mailjet.template.id.agent.prospect}")
    private Integer templateIDAgentProspect;
    @Value("${mailjet.template.id.account.deleted}")
    private Integer templateIdAccountDeleted;

    @Async
    public void sendAsyncMail(String from, String to, String replyToEmail, String subject, String msg) {
        sendMail(from, to, replyToEmail, subject, msg);
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

    @Async
    public void sendMailJetApi(
            String fromEmail, String fromName,
            String toEmail, String toName,
            String replyToEmail, String replyToName,
            String ccEmail, String ccName,
            String bccEmail, String bccName,
            String subject,
            Map<String, String> variables,
            int templateID
    ) {
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
        variables.put(LAST_NAME, user.getLastName());
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
    public void sendMailNotificationAfterDeny(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put(FIRST_NAME, user.getFirstName());
        sendMailJetApi(emailFrom, null, user.getEmail(), user.getFullName(), null, null, null, null, null, null, null, variables, templateIDMessageNotification);
    }

    public void sendNotification(String email) {
        Map<String, String> variables = new HashMap<>();
        variables.put(ADRESSE_BETATESTEUR, email);
        this.sendMailJetApi(emailFrom, null, emailAdmin1, null, null, null, emailAdmin2, null, null, null, null, variables, templateIDAgentProspect);
    }

    private void sendMail(final String from, final String to, final String replyToEmail, final String subject, final String msg) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setReplyTo(new javax.mail.Address[]
                    {
                            new InternetAddress(replyToEmail)
                    });
            mimeMessage.setSubject(MimeUtility.encodeText(subject, "UTF-8", "Q"));
            mimeMessage.setContent(msg, "text/html; charset=utf-8");
        };
        try {
            mailSender.send(messagePreparator);
        } catch (MailException e) {
            log.error(e.getMessage(), e.getCause());
        }
    }
}
