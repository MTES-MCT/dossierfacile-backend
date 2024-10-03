package fr.dossierfacile.api.dossierfacileapiowner.mail;

import brevo.ApiClient;
import brevo.ApiException;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailTo;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private final TenantCommonRepository tenantCommonRepository;
    @Value("${brevo.apikey}")
    private String sendinblueApiKey;
    @Value("${brevo.template.id.welcome}")
    private Long templateIDWelcome;
    @Value("${brevo.template.id.new.password}")
    private Long templateIdNewPassword;
    @Value("${owner.url}")
    private String ownerUrl;
    @Value("${brevo.template.id.applicant.validated}")
    private Long templateIdApplicantValidated;
    @Value("${brevo.template.id.new.applicant.validated}")
    private Long templateIdNewApplicantValidated;
    @Value("${brevo.template.id.new.applicant.not.validated}")
    private Long templateIdNewApplicantNotValidated;
    @Value("${brevo.template.id.validated.property}")
    private Long templateIdValidatedProperty;
    @Value("${brevo.template.id.follow-up.validated.property}")
    private Long templateIdFollowUpAfterValidatedProperty;

    @Value("${property.path}")
    private String propertyPath;

    private void sendTransactionalEmail(Long templateId, User to, Object emailParams) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(sendinblueApiKey);

        SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
        ///sendSmtpEmailTo.setName(to.getFullName());
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
                PropertyApplicantMailParams.builder()
                        .ownerLastname(owner.getLastName())
                        .ownerFirstname(owner.getFirstName())
                        .tenantName(tenant.get().getFullName())
                        .tenantEmail(tenant.get().getEmail())
                        .propertyName(associatedProperty.getName())
                        .propertyUrl(ownerUrl + propertyPath + associatedProperty.getId())
                        .build());
    }

    @Override
    public void sendEmailNewApplicantValidated(Tenant tenant, Owner owner, Property property) {
        sendTransactionalEmail(templateIdNewApplicantValidated, owner,
                PropertyApplicantMailParams.builder()
                        .ownerLastname(owner.getLastName())
                        .ownerFirstname(owner.getFirstName())
                        .tenantName(tenant.getFullName())
                        .tenantEmail(tenant.getEmail())
                        .propertyName(property.getName())
                        .propertyUrl(ownerUrl + propertyPath + property.getId())
                        .build());
    }

    @Override
    public void sendEmailNewApplicantNotValidated(Tenant tenant, Owner owner, Property property) {
        sendTransactionalEmail(templateIdNewApplicantNotValidated, owner,
                PropertyApplicantMailParams.builder()
                        .ownerLastname(owner.getLastName())
                        .ownerFirstname(owner.getFirstName())
                        .tenantName(tenant.getFullName())
                        .tenantEmail(tenant.getEmail())
                        .propertyName(property.getName())
                        .propertyUrl(ownerUrl + propertyPath + property.getId())
                        .build());
    }

    @Async
    @Override
    public void sendEmailValidatedProperty(User user, Property property) {
        sendTransactionalEmail(templateIdValidatedProperty, user,
                ValidatedPropertyParams.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .propertyUrl(ownerUrl + propertyPath + property.getId()));
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
