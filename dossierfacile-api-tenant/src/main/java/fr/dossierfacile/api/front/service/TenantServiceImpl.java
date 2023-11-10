package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.MailSentLimitException;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.model.TenantUpdate;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
@Validated
public class TenantServiceImpl implements TenantService {
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final LogService logService;
    private final MailService mailService;
    private final PartnerCallBackService partnerCallBackService;
    private final RegisterFactory registerFactory;
    private final TenantCommonRepository tenantRepository;
    private final KeycloakService keycloakService;
    private final UserApiService userApiService;

    @Override
    public <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step) {
        return registerFactory.get(step.getLabel()).saveStep(tenant, formStep);
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
            throw new IllegalStateException("Tenant " + Obfuscator.email(tenant.getEmail()) + " already exists (same mail)");
        }
        if (tenant.getKeycloakId() != null && tenantRepository.findByKeycloakId(tenant.getKeycloakId()) != null) {
            throw new IllegalStateException("Tenant " + Obfuscator.email(tenant.getEmail()) + " already exists (same keycloak id)");
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
        return tenantRepository.findById(id).orElse(null);
    }

    @Override
    public Tenant findByKeycloakId(String keycloakId) {
        return tenantRepository.findByKeycloakId(keycloakId);
    }

    @Override
    @Transactional
    public Tenant registerFromKeycloakUser(KeycloakUser kcUser, String partner, AcquisitionData acquisitionData) {
        // check user still exists in keycloak
        if (keycloakService.getKeyCloakUser(kcUser.getKeycloakId()) == null) {
            throw new TenantNotFoundException("User doesn't exist anymore in KC - token is out-of-date");
        }
        Tenant tenant = create(Tenant.builder()
                .tenantType(TenantType.CREATE)
                .keycloakId(kcUser.getKeycloakId())
                .email(kcUser.getEmail())
                .firstName(kcUser.getGivenName())
                .lastName(kcUser.getFamilyName())
                .preferredName(kcUser.getPreferredUsername())
                .franceConnect(kcUser.isFranceConnect())
                .honorDeclaration(false)
                .build());

        if (acquisitionData != null) {
            tenant.setAcquisitionCampaign(acquisitionData.campaign());
            tenant.setAcquisitionSource(acquisitionData.source());
            tenant.setAcquisitionMedium(acquisitionData.medium());
        }

        if (!kcUser.isEmailVerified()) {
            // createdAccount without verified email should be deactivated
            keycloakService.disableAccount(kcUser.getKeycloakId());
            mailService.sendEmailConfirmAccount(tenant, confirmationTokenService.createToken(tenant));
        }
        if (partner != null) {
            userApiService.findByName(partner)
                    .ifPresent(userApi -> partnerCallBackService.registerTenant(null, tenant, userApi));
        }

        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);

        if (kcUser.isFranceConnect()) {
            logService.saveLog(LogType.FC_ACCOUNT_CREATION, tenant.getId());
        } else {
            logService.saveLog(LogType.ACCOUNT_CREATED_VIA_KC, tenant.getId());
        }
        return tenantRepository.save(tenant);
    }

    @Override
    public List<TenantUpdate> findTenantUpdateByCreatedAndPartner(LocalDateTime from, UserApi userApi, Long limit) {
        return tenantRepository.findTenantUpdateByCreationDateAndPartner(from, userApi.getId(), limit);
    }

    @Override
    public List<TenantUpdate> findTenantUpdateByLastUpdateAndPartner(LocalDateTime since, UserApi userApi, Long limit, boolean includeDeleted) {
        return includeDeleted? tenantRepository.findTenantUpdateWithDeletedByLastUpdateAndPartner(since, userApi.getId(), limit) :
                tenantRepository.findTenantUpdateByLastUpdateAndPartner(since, userApi.getId(), limit);
    }

    @Override
    public void sendFileByMail(Tenant tenant, String email, String shareType) {
        String token = UUID.randomUUID().toString();
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        List<ApartmentSharingLink> existingASL = apartmentSharingLinkRepository.findByApartmentSharingAndCreationDateIsAfter(tenant.getApartmentSharing(), date );
        if (existingASL.size() > 10) {
            log.info("Daily limit reached for file sharing by mail");
            throw new MailSentLimitException();
        }

        ApartmentSharingLink apartmentSharingLink = ApartmentSharingLink.builder()
                .apartmentSharing(tenant.getApartmentSharing())
                .disabled(false)
                .fullData("full".equals(shareType))
                .token(token)
                .linkType(ApartmentSharingLinkType.MAIL)
                .email(email)
                .mailSent(false).build();
        apartmentSharingLink = apartmentSharingLinkRepository.save(apartmentSharingLink);
        String url = "/file/" + apartmentSharingLink.getToken();
        if ("resume".equals(shareType)) {
            url = "/public-file/" + apartmentSharingLink.getToken();
        }
        mailService.sendFileByMail(url, email, tenant.getFirstName(), tenant.getFullName(), tenant.getEmail());
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return tenantRepository.findByEmail(email);
    }
}
