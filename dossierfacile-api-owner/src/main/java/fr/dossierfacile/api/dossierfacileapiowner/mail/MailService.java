package fr.dossierfacile.api.dossierfacileapiowner.mail;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;

import java.util.List;

public interface MailService {
    void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken);

    void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken);

    void sendEmailApplicantValidated(Property associatedProperty, List<Long> tenantIds);

    void sendEmailNewApplicant(Tenant tenant, Owner owner, Property property);

    void sendEmailValidatedProperty(User user, Property property);

    void sendEmailFollowUpValidatedProperty(User user, Property property);
}
