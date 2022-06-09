package fr.dossierfacile.api.dossierfacileapiowner.mail;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailTo;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class MailServiceImplDev implements MailService {
    @Value("${sendinblue.apikey}")
    private String sendinblueApiKey;
    @Value("${sendinblue.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${sendinblue.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${owner.url}")
    private String ownerUrl;

    @Async
    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(sendinblueApiKey);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());

        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        sendSmtpEmail.templateId(templateIDWelcome);

        ConfirmMailParams confirmMailParams =
                new ConfirmMailParams(ownerUrl + "/confirmerCompte/" + confirmationToken.getToken());
        sendSmtpEmail.params(confirmMailParams);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("message: {}", sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(sendinblueApiKey);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setEmail(user.getEmail());

        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        sendSmtpEmail.templateId(templateIdNewPassword);

        NewPasswordMailParams newPasswordMailParams = new NewPasswordMailParams(user.getFirstName(),
                ownerUrl + "/modifier-mot-de-passe/" + passwordRecoveryToken.getToken());
        sendSmtpEmail.params(newPasswordMailParams);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("message: {}", sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

}
