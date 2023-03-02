package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.form.SubscriptionApartmentSharingOfTenantForm;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.PropertyApartmentSharingRepository;
import fr.dossierfacile.api.front.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.model.TenantUpdate;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class TenantServiceImpl implements TenantService {
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final DocumentService documentService;
    private final LogService logService;
    private final MailService mailService;
    private final GuarantorService guarantorService;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final PropertyService propertyService;
    private final RegisterFactory registerFactory;
    private final TenantCommonRepository tenantRepository;
    private final KeycloakService keycloakService;
    private final UserApiService userApiService;

    @Override
    public <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step) {
        return registerFactory.get(step.getLabel()).saveStep(tenant, formStep);
    }

    @Override
    public void subscribeApartmentSharingOfTenantToPropertyOfOwner(String propertyToken, SubscriptionApartmentSharingOfTenantForm subscriptionApartmentSharingOfTenantForm, Tenant tenant) {
        if (tenant.getTenantType() == TenantType.CREATE) {
            Property property = propertyService.getPropertyByToken(propertyToken);
            PropertyApartmentSharing propertyApartmentSharing = propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()).orElse(
                    PropertyApartmentSharing.builder()
                            .accessFull(subscriptionApartmentSharingOfTenantForm.getAccess())
                            .token(subscriptionApartmentSharingOfTenantForm.getAccess() ? tenant.getApartmentSharing().getToken() : tenant.getApartmentSharing().getTokenPublic())
                            .property(property)
                            .apartmentSharing(tenant.getApartmentSharing())
                            .build()
            );
            propertyApartmentSharingRepository.save(propertyApartmentSharing);
        } else {
            throw new IllegalStateException("Tenant is not the main tenant");
        }
    }

    @Override
    public void updateLastLoginDateAndResetWarnings(Tenant tenant) {
        tenant.setLastLoginDate(LocalDateTime.now());
        tenant.setWarnings(0);
        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.RETURNED_ACCOUNT);
        }
        log.info("Updating last_login_date of tenant with ID [" + tenant.getId() + "]");
        tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public Tenant create(Tenant tenant) {
        if (tenantRepository.findByEmail(tenant.getEmail()).isPresent()) {
            throw new IllegalStateException("Tenant " + Obfuscator.email(tenant.getEmail()) + " already exists");
        }
        tenant.setApartmentSharing(new ApartmentSharing(tenant));
        apartmentSharingRepository.save(tenant.getApartmentSharing());
        return tenantRepository.save(tenant);
    }

    @Override
    public EmailExistsModel emailExists(EmailExistsForm emailExistsForm) {
        return EmailExistsModel.builder()
                .email(emailExistsForm.getEmail())
                .exists(tenantRepository.existsByEmail(emailExistsForm.getEmail()))
                .build();
    }

    @Override
    public Tenant findById(Long id) {
        return tenantRepository.findById(id).get();
    }

    @Override
    public Tenant findByKeycloakId(String keycloakId) {
        return tenantRepository.findByKeycloakId(keycloakId);
    }

    @Transactional
    @Override
    public void processWarningsBatch(LocalDateTime localDateTime, int warnings, Pageable page) {
        Page<Tenant> tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime, warnings);

        switch (warnings) {
            case 0 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for FIRST time by email");
            case 1 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for SECOND time by email");
            case 2 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants whose documents will be deleted");
        }

        tenantList.stream().forEach(t -> {
            try {
                handleTenantWarning(t, warnings);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Sentry.captureException(e);
            }
        });
    }

    private void handleTenantWarning(Tenant t, int warnings) {
        switch (warnings) {
            case 0 -> {
                log.info("accountWarnings. FIRST warning for tenant with ID [" + t.getId() + "]");
                t.setWarnings(1);
                mailService.sendEmailFirstWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
            }
            case 1 -> {
                log.info("accountWarnings. SECOND warning for tenant with ID [" + t.getId() + "]");
                t.setWarnings(2);
                mailService.sendEmailSecondWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
            }
            case 2 -> {
                log.info("accountWarnings. Documents deletion for tenant with ID [" + t.getId() + "]");
                t.setWarnings(0);
                t.setConfirmationToken(null);
                t.setHonorDeclaration(false);
                t.setStatus(TenantFileStatus.ARCHIVED);
                t.setZipCode("");
                t.setClarification("");

                ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(t).orElseThrow(() -> new ConfirmationTokenNotFoundException(t.getId()));
                confirmationTokenRepository.delete(confirmationToken);

                documentService.deleteAllDocumentsAssociatedToTenant(t);
                guarantorService.deleteAllGuaratorsAssociatedToTenant(t);
                logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, t.getId());
                partnerCallBackService.sendCallBack(t, PartnerCallBackType.ARCHIVED_ACCOUNT);
            }
        }
        tenantRepository.save(t);
    }

    @Override
    @Transactional
    public Tenant registerFromKeycloakUser(KeycloakUser kcUser, String partner) {
        Tenant tenant = Tenant.builder()
                .tenantType(TenantType.CREATE)
                .keycloakId(kcUser.getKeycloakId())
                .email(kcUser.getEmail())
                .firstName(kcUser.getGivenName())
                .lastName(kcUser.getFamilyName())
                .preferredName(kcUser.getPreferredUsername())
                .franceConnect(kcUser.isFranceConnect())
                .honorDeclaration(false)
                .build();
        tenant = create(tenant);

        if (!kcUser.isEmailVerified()) {
            // createdAccount without verified email should be deactivated
            keycloakService.disableAccount(kcUser.getKeycloakId());
            mailService.sendEmailConfirmAccount(tenant, confirmationTokenService.createToken(tenant));
        }
        Optional<UserApi> userApi = userApiService.findByName(partner);
        if (userApi.isPresent()) {
            partnerCallBackService.registerTenant(null, tenant, userApi.get());
        }

        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);

        logService.saveLog(LogType.ACCOUNT_CREATED, tenant.getId());
        return tenantRepository.save(tenant);
    }

    @Override
    public List<TenantUpdate> findTenantUpdateByLastUpdateIntervalAndPartner(LocalDateTime updateDateTimeSince, LocalDateTime updateDateTimeTo, UserApi partner) {
        return tenantRepository.findTenantUpdateByLastUpdateIntervalAndPartner(updateDateTimeSince, updateDateTimeTo, partner.getId());
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return tenantRepository.findByEmail(email);
    }
}
