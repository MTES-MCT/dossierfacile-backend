package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserApiDto;
import fr.dossierfacile.common.dto.mail.UserDto;
import fr.dossierfacile.api.front.form.ContactForm;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;

public interface MailService {
    void sendEmailConfirmAccount(UserDto user, ConfirmationToken confirmationToken);

    void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken);

    void sendEmailForFlatmates(User flatmate, User guest, PasswordRecoveryToken passwordRecoveryToken, ApplicationType applicationType);

    void sendEmailAccountDeleted(UserDto user);

    void sendEmailAccountCompleted(TenantDto tenant);

    void sendEmailWhenEmailAccountNotYetValidated(User user, ConfirmationToken confirmationToken);

    void sendEmailWhenAccountNotYetCompleted(User user);

    void sendEmailWhenAccountIsStillDeclined(User user);

    void sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(User user);

    void sendEmailToSupport(ContactForm form);

    void sendEmailWelcomeForPartnerUser(UserDto user, UserApiDto userApi);

    void sendFileByMail(String url, String email, String tenantName, String fullName, String tenantEmail);

    void sendEmailPartnerAccessRevoked(Tenant receiver, UserApi userApi, Tenant revocationRequester);

    void sendDefaultEmailExpiredToken(String email, OperationAccessToken token);
}
