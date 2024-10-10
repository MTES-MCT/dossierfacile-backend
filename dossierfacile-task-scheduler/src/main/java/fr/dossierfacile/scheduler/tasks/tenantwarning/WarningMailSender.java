package fr.dossierfacile.scheduler.tasks.tenantwarning;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailTo;
import com.google.common.base.Strings;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarningMailSender {

    private static final String TENANT_BASE_URL_KEY = "tenantBaseUrl";
    private static final String TENANT_BASE_URL_KEY_DEPRECATED = "sendinBlueUrlDomain";
    private static final String OWNER_BASE_URL_KEY = "ownerBaseUrl";

    private final TransactionalEmailsApi apiInstance;
    @Value("${tenant.base.url}")
    private String tenantBaseUrl;
    @Value("${owner.base.url}")
    private String ownerBaseUrl;
    @Value("${brevo.owner.url.domain:proprietaire.dossierfacile.fr}")
    private String sendinBlueOwnerUrlDomain;
    @Value("${brevo.template.id.first.warning.for.deletion.of.documents}")
    private Long templateFirstWarningForDeletionOfDocuments;
    @Value("${brevo.template.id.second.warning.for.deletion.of.documents}")
    private Long templateSecondWarningForDeletionOfDocuments;
    @Value("${brevo.template.id.first.warning.for.deletion.of.owner:144}")
    private Long templateFirstWarningForDeletionOfOwner;
    @Value("${brevo.template.id.second.warning.for.deletion.of.owner:146}")
    private Long templateSecondWarningForDeletionOfOwner;
    @Value("${brevo.template.id.account.deleted.owner:145}")
    private Long templateOwnerDeleted;

    public void sendEmailFirstWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        if (isNotBlank(user.getEmail())) {
            log.info("Sending FIRST warning to tenant {}", user.getId());
            sendTenantWarningMail(user, confirmationToken, templateFirstWarningForDeletionOfDocuments);
        }
    }

    public void sendEmailSecondWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken) {
        if (isNotBlank(user.getEmail())) {
            log.info("Sending SECOND warning to tenant {}", user.getId());
            sendTenantWarningMail(user, confirmationToken, templateSecondWarningForDeletionOfDocuments);
        }
    }

    private void sendTenantWarningMail(User user, ConfirmationToken confirmationToken, Long templateId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put(TENANT_BASE_URL_KEY, tenantBaseUrl);
        variables.put(TENANT_BASE_URL_KEY_DEPRECATED, tenantBaseUrl.replaceAll("https?://", ""));
        sendWarningMail(user, templateId, variables);
    }

    private void sendOwnerWarningMail(User user, ConfirmationToken confirmationToken, Long templateId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        variables.put("confirmToken", confirmationToken.getToken());
        variables.put(OWNER_BASE_URL_KEY, ownerBaseUrl);
        sendWarningMail(user, templateId, variables);
    }

    private void sendWarningMail(User user, Long templateId, Map<String, String> variables) {

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
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

    public void sendEmailFirstWarningForDeletionOfOwner(User user, ConfirmationToken confirmationToken) {
        if (isNotBlank(user.getEmail())) {
            log.info("Sending FIRST warning to owner {}", user.getId());
            sendOwnerWarningMail(user, confirmationToken, templateFirstWarningForDeletionOfOwner);
        }
    }

    public void sendEmailSecondWarningForDeletionOfOwner(User user, ConfirmationToken confirmationToken) {
        if (isNotBlank(user.getEmail())) {
            log.info("Sending SECOND warning to owner {}", user.getId());
            sendOwnerWarningMail(user, confirmationToken, templateSecondWarningForDeletionOfOwner);
        }
    }

    public void sendEmailOwnerDeleted(User user) {
        if (isEmpty(user.getEmail())) {
            return;
        }
        log.info("User deleted {}", user.getId());
        Map<String, String> variables = new HashMap<>();
        variables.put("PRENOM", user.getFirstName());
        variables.put("NOM", Strings.isNullOrEmpty(user.getPreferredName()) ? user.getLastName() : user.getPreferredName());
        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());
        if (!Strings.isNullOrEmpty(user.getFullName())) {
            sendSmtpEmailTo.setName(user.getFullName());
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.templateId(templateOwnerDeleted);
        sendSmtpEmail.params(variables);
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }
}
