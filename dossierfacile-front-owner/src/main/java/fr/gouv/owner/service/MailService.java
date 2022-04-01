package fr.gouv.owner.service;


import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Prospect;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.owner.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MailService {

    private static final String EMAIL = "Email";
    private static final String TENANT_FIRST_NAME = "tenantFirstName";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String OPERATOR = "operator";
    private static final String GMAIL_COM = "@gmail.com";
    private static final String FIRST_NAME = "firstName";
    private static final String PARAM_EMAIL = "?email=";
    private static final String PARAM_FIRST_NAME = "&firstName=";
    private static final String PARAM_LAST_NAME = "&lastName=";
    private static final String URL_PROSPECT_INVITE = "/prospect/invite/";
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private MailjetClient client;
    @Value("${email.from}")
    private String emailFrom;
    @Value("${email.to.send.notification}")
    private String emailToSendNotification;
    @Value("${domain.url}")
    private String domainURL;
    @Value("${mailjet.template.id.prospect.invitation}")
    private Integer templateIDProspectInvitation;
    @Value("${mailjet.template.id.message.notification}")
    private Integer templateIDMessageNotification;
    @Value("${email.from.admin1}")
    private String emailAdmin1;
    @Value("${email.from.admin1}")
    private String emailAdmin2;
    @Value("${mailjet.template.id.agent.prospect}")
    private Integer templateIDAgentProspect;
    @Value("${mailjet.template.id.enterprise}")
    private Integer templateIDEnterprise;
    @Value("${email.from.invite.property}")
    private String emailToInvite;
    @Value("${fake.invitation.prospect}")
    private boolean fakeInvitation;
    @Value("${mailjet.template.id.reminder.visit.prospect}")
    private Integer mailjetTemplateIdReminderVisitProspect;
    @Value("${mailjet.template.id.reminder.welcome.prospect}")
    private Integer mailjetTemplateIdReminderWelcomeProspect;
    @Value("${mailjet.template.id.owner.prospect}")
    private Integer mailjetTemplateIdOwnerProspect;
    @Value("${domain.protocol}")
    private String domainProtocol;
    @Autowired
    private TenantRepository tenantRepository;
    @Value("${mailjet.template.id.owner.welcome}")
    private Integer templateIDOwnerWelcome;
    @Value("${mailjet.template.id.account.invalidate}")
    private int templateIDAccountInvalidate;
    @Value("${mailjet.template.id.alert.update.account}")
    private int templateIdAlertUpdateAccount;

    @Async
    public void sendAsyncMail(String from, String to, String replyToEmail, String subject, String msg) {
        sendMail(from, to, null, replyToEmail, subject, msg);
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
        } catch (MailjetSocketTimeoutException e) {
            log.error("MailjetSocketTimeoutException", e);
        }
    }

    @Async
    public void sendMailJetApi(
            String fromEmail, String fromName,
            String toEmail, String toName,
            String subject,
            String content
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

        //subject
        if (subject != null) {
            messagge.put(Emailv31.Message.SUBJECT, subject);
        }

        //templateID
        messagge.put(Emailv31.Message.TEXTPART, content);

        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(messagge));
        try {
            response = client.post(request);
            log.info("ResponseStatus: {}", response.getStatus());
            log.info("Response: {}", response.getData());
        } catch (MailjetException e) {
            log.error("MailjetException", e);
        } catch (MailjetSocketTimeoutException e) {
            log.error("MailjetSocketTimeoutException", e);
        }
    }

    public void sendMail(final String from, final String to, final String bcc, final String replyToEmail, final String subject, final String msg) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            if (bcc != null) {
                mimeMessage.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
            }
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



    @Async
    public void sendMailNotificationNewMessageSleepMode(Tenant tenant) {
        Map<String, String> variables = new HashMap<>();
        variables.put(FIRST_NAME, tenant.getFirstName());
        sendMailJetApi(emailFrom, null, tenant.getEmail(), tenant.getFullName(), null, null, null, null, null, null, null, variables, templateIDMessageNotification);
    }

    public void sendEmailForGiveAccessToProperty(Prospect prospect) {
        Map<String, String> variables = new HashMap<>();
        String tokenLink = domainURL + URL_PROSPECT_INVITE + prospect.getProperty().getToken() + PARAM_EMAIL + prospect.getEmail() + PARAM_FIRST_NAME + prospect.getFirstName() + PARAM_LAST_NAME + prospect.getLastName();
        variables.put(FIRST_NAME, prospect.getFirstName());
        variables.put(PROPERTY_NAME, prospect.getProperty().getName());
        variables.put("link", tokenLink);
        if (fakeInvitation) {
            String prefix = emailToInvite.split("@")[0];
            String email = prefix + "+" + prospect.getEmail().split("@")[0] + GMAIL_COM;
            this.sendMailJetApi(emailFrom, null, email, OPERATOR, null, null, null, null, null, null, null, variables, templateIDProspectInvitation);
        } else {
            this.sendMailJetApi(emailFrom, null, prospect.getEmail(), prospect.getFullName(), null, null, null, null, null, null, null, variables, templateIDProspectInvitation);
        }
    }

    public void sendNotification(String email) {
        Map<String, String> variables = new HashMap<>();
        variables.put("ADRESSE_BETATESTEUR", email);
        this.sendMailJetApi(emailFrom, null, emailAdmin1, null, null, null, emailAdmin2, null, null, null, null, variables, templateIDAgentProspect);
    }



    public void sendMailNotificationEmailNotProcessed(Long internalDate, String snippet) {
        DateFormat formatter = new SimpleDateFormat("EEEE dd/MM/yyyy hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(internalDate);
        String message = "snippet: " + snippet + "\n\n\n date: " + formatter.format(calendar.getTime());
        this.sendMailJetApi(emailFrom, "Locatio", emailToSendNotification, "", "Mail no processed", message);
    }



    public void sendEmailForGiveAccessToPropertyOwner(Prospect prospect) {
        Map<String, String> variables = new HashMap<>();
        String tokenLink = domainURL + URL_PROSPECT_INVITE + prospect.getProperty().getToken() + PARAM_EMAIL + prospect.getEmail() + PARAM_FIRST_NAME + prospect.getFirstName() + PARAM_LAST_NAME + prospect.getLastName();
        variables.put("nameOwner", prospect.getProperty().getOwner().getFullName());
        variables.put("link", tokenLink);
        variables.put(FIRST_NAME, prospect.getTenant() != null ? prospect.getTenant().getFirstName() : prospect.getFirstName());
        if (fakeInvitation) {
            String prefix = emailToInvite.split("@")[0];
            String email = prefix + "+" + prospect.getEmail().split("@")[0] + GMAIL_COM;
            this.sendMailJetApi(emailFrom, null, email, OPERATOR, null, null, null, null, null, null, null, variables, mailjetTemplateIdOwnerProspect);
        } else {
            this.sendMailJetApi(emailFrom, null, prospect.getEmail(), prospect.getFullName(), null, null, null, null, null, null, null, variables, mailjetTemplateIdOwnerProspect);
        }

    }

    public void sendEmailOwnerWelcome(Owner owner) {
        Map<String, String> variables = new HashMap<>();
        variables.put("nameOwner", owner.getFullName());
        this.sendMailJetApi(emailFrom, null, owner.getEmail(), owner.getFullName(), null, null, null, null, null, null, null, variables, templateIDOwnerWelcome);
    }


}
