package fr.dossierfacile.api.dossierfacileapiowner.mail;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;

public interface MailService {
    void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken);

    void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken);

}
