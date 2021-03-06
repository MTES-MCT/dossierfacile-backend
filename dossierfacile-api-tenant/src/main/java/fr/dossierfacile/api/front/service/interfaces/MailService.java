package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ApplicationType;

public interface MailService {
    void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken);

    void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken);

    void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType);

    void sendEmailAccountDeleted(User user);

    void sendEmailAccountCompleted(User user);

    void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken);

    void sendEmailWhenAccountNotYetCompleted(User user);

    void sendEmailWhenAccountIsStillDeclined(User user);

    void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailFirstWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken);

    void sendEmailSecondWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken);

    void sendEmailToSupport(ContactForm form);
}
