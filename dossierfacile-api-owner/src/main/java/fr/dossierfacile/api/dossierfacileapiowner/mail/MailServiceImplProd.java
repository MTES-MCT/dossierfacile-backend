package fr.dossierfacile.api.dossierfacileapiowner.mail;

import com.mailjet.client.resource.Emailv31;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.mailjet.client.resource.Emailv31.Message.EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("prod")
public class MailServiceImplProd implements MailService {
    @Value("${email.from}")
    private String emailFrom;
    @Value("${mailjet.template.id.welcome}")
    private Integer templateIDWelcome;
    @Value("${mailjet.template.id.new.password}")
    private Integer templateIdNewPassword;

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

    private void sendMailJetApi(MailJetModel mailjetModel) {
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

        log.info("message: {}", message);
    }
}
