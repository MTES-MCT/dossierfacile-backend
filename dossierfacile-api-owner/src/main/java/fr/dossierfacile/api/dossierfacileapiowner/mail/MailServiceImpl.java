package fr.dossierfacile.api.dossierfacileapiowner.mail;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private final TenantCommonRepository tenantCommonRepository;
    @Value("${sendinblue.apikey}")
    private String sendinblueApiKey;
    @Value("${sendinblue.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${sendinblue.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${owner.url}")
    private String ownerUrl;
    @Value("${sendinblue.template.id.applicant.validated}")
    private Long templateIdApplicantValidated;
    @Value("${sendinblue.template.id.new.applicant}")
    private Long templateIdNewApplicant;
    @Value("${sendinblue.template.id.validated.property}")
    private Long templateIdValidatedProperty;

    @Value("${sendinblue.template.id.follow-up.validated.property}")
    private Long templateIdFollowUpAfterValidatedProperty;


    private void sendTransactionalEmail(Long templateId, User to, Object emailParams) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(sendinblueApiKey);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        sendSmtpEmailTo.setName(to.getFullName());
        sendSmtpEmailTo.setEmail(to.getEmail());

        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

        sendSmtpEmail.templateId(templateId);

        sendSmtpEmail.params(emailParams);

        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            log.error("Email api exception", e);
        }
    }

    @Async
    @Override
    public void sendEmailConfirmAccount(User user, ConfirmationToken confirmationToken) {
        sendTransactionalEmail(templateIDWelcome, user,
                new ConfirmMailParams(ownerUrl + "/confirmerCompte/" + confirmationToken.getToken()));
    }

    @Async
    @Override
    public void sendEmailNewPassword(User user, PasswordRecoveryToken passwordRecoveryToken) {
        sendTransactionalEmail(templateIdNewPassword, user,
                new NewPasswordMailParams(user.getFirstName(), ownerUrl + "/modifier-mot-de-passe/" + passwordRecoveryToken.getToken()));
    }

    @Override
    public void sendEmailApplicantValidated(Property associatedProperty, List<Long> tenantIds) {
        List<Tenant> tenants = tenantCommonRepository.findAllById(tenantIds);
        Optional<Tenant> tenant = tenants.stream().filter(t -> TenantType.CREATE.equals(t.getTenantType())).findAny();
        if (tenant.isEmpty()) {
            log.error("Unable to find CREATE tenants {}", tenantIds);
            return;
        }
        Owner owner = associatedProperty.getOwner();
        sendTransactionalEmail(templateIdApplicantValidated, owner,
                ApplicantValidatedMailParams.builder()
                        .ownerLastname(owner.getLastName())
                        .ownerFirstname(owner.getFirstName())
                        .tenantName(tenant.get().getFullName())
                        .build());
    }

    @Override
    public void sendEmailNewApplicant(Tenant tenant, Owner owner) {
        sendTransactionalEmail(templateIdNewApplicant, owner,
                NewApplicantMailParams.builder()
                        .ownerLastname(owner.getLastName())
                        .ownerFirstname(owner.getFirstName())
                        .tenantName(tenant.getFullName())
                        .build());
    }
    @Async
    @Override
    public void sendEmailValidatedProperty(User user, Property property) {
        sendTransactionalEmail(templateIdValidatedProperty, user,
                ValidatedPropertyParams.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .propertyUrl(ownerUrl + "/consulte-propriete/" + property.getId()));
    }

    @Override
    public void sendEmailFollowUpValidatedProperty(User user, Property property) {
        sendTransactionalEmail(templateIdFollowUpAfterValidatedProperty, user,
                ValidatedPropertyParams.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .propertyName(property.getName()));
    }


}
