package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.MailSentLimitException;
import fr.dossierfacile.api.front.exception.ResendLinkTooShortException;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.form.ShareFileByLinkForm;
import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.model.TenantUpdate;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static fr.dossierfacile.common.enums.ApartmentSharingLinkType.MAIL;

@Slf4j
@Service
@RequiredArgsConstructor
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
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;
    private final TenantMapperForMail tenantMapperForMail;

    @Override
    public <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step) {
        return registerFactory.get(step.getLabel()).saveStep(tenant, formStep);
    }

    @Override
    public void updateLastLoginDateAndResetWarnings(Tenant tenantToUpdate) {
        LocalDateTime currentTime = LocalDateTime.now();
        for (Tenant tenant : tenantToUpdate.getApartmentSharing().getTenants()) {
            if (Objects.equals(tenant.getId(), tenantToUpdate.getId())
                    || StringUtils.isBlank(tenant.getEmail())
                    || tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
                tenant.setLastLoginDate(currentTime);
                tenant.setWarnings(0);
                if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
                    tenant.setStatus(TenantFileStatus.INCOMPLETE);
                    logService.saveLog(LogType.ACCOUNT_RETURNED, tenant.getId());
                    partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.RETURNED_ACCOUNT);
                }
                log.info("Updating last_login_date of tenant with ID [" + tenant.getId() + "]");
                tenantRepository.save(tenant);
            }
        }
    }

    @Override
    @Transactional
    public void doNotArchive(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService.findByToken(token);
        Tenant tenant = tenantRepository.getReferenceById(confirmationToken.getUser().getId());
        tenant.setConfirmationToken(null);
        updateLastLoginDateAndResetWarnings(tenant);
        logService.saveLog(LogType.DO_NOT_ARCHIVE, tenant.getId());
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
            TransactionalUtil.afterCommit(() -> mailService.sendEmailConfirmAccount(tenantMapperForMail.toDto(tenant), confirmationTokenService.createToken(tenant)));

        }
        if (partner != null) {
            userApiService.findByName(partner)
                    .ifPresent(userApi -> partnerCallBackService.registerTenant(tenant, userApi));
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
    public List<TenantUpdate> findTenantUpdateByLastUpdateAndPartner(LocalDateTime since, UserApi userApi, Long limit, boolean includeDeleted, boolean includeRevoked) {
        return tenantRepository.findTenantUpdateByLastUpdateAndPartner(since, userApi.getId(), limit, includeDeleted, includeRevoked);
    }

    @Override
    public void sendFileByMail(Tenant tenant, ShareFileByMailForm form) {
        UUID token = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        List<ApartmentSharingLink> existingASL = apartmentSharingLinkRepository.findByApartmentSharingAndCreationDateIsAfterAndDeletedIsFalse(tenant.getApartmentSharing(), date);
        if (existingASL.size() > 10) {
            log.info("Daily limit reached for file sharing by mail");
            throw new MailSentLimitException();
        }

        ApartmentSharingLink apartmentSharingLink = ApartmentSharingLink.builder()
                .apartmentSharing(tenant.getApartmentSharing())
                .disabled(false)
                .lastSentDatetime(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(form.getDaysValid()))
                .title(form.getTitle())
                .createdBy(tenant.getId())
                .fullData(form.isFullData())
                .token(token)
                .linkType(ApartmentSharingLinkType.MAIL)
                .email(form.getEmail())
                .build();

        String url = "/file/" + apartmentSharingLink.getToken();
        if (!form.isFullData()) {
            url = "/public-file/" + apartmentSharingLink.getToken();
        }
        mailService.sendFileByMail(url, form.getEmail(), form.getMessage(), tenant.getFirstName(), tenant.getFullName(), tenant.getEmail());

        // save after successfully sent
        apartmentSharingLinkRepository.save(apartmentSharingLink);
    }

    @Override
    public String createSharingLink(Tenant tenant, ShareFileByLinkForm form) {
        UUID token = UUID.randomUUID();
        ApartmentSharingLink apartmentSharingLink = ApartmentSharingLink.builder()
                .apartmentSharing(tenant.getApartmentSharing())
                .disabled(false)
                .expirationDate(LocalDateTime.now().plusDays(form.getDaysValid()))
                .title(form.getTitle())
                .createdBy(tenant.getId())
                .fullData(form.isFullData())
                .token(token)
                .linkType(ApartmentSharingLinkType.LINK)
                .build();
        apartmentSharingLinkRepository.save(apartmentSharingLink);

        String path = form.isFullData() ? "/file/" : "/public-file/";
        return path + token;
    }

    @Override
    public void resendLink(Long id, Tenant tenant) {
        ApartmentSharingLink link = apartmentSharingLinkRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!Objects.equals(tenant.getApartmentSharing().getId(), link.getApartmentSharing().getId())) {
            throw new AccessDeniedException("Access Denied");
        }
        if (link.isDisabled() || link.getLinkType() != MAIL) {
            throw new IllegalStateException("A disabled link cannot be sent");
        }
        if (link.getLastSentDatetime() != null && link.getLastSentDatetime().isAfter(LocalDateTime.now().minusHours(1))) {
            log.info("Email has been sent previously from less than one hour");
            throw new ResendLinkTooShortException();
        }
        String url = (link.isFullData() ? "/file/" : "/public-file/") + link.getToken();
        mailService.sendFileByMail(url, link.getEmail(), "", tenant.getFirstName(), tenant.getFullName(), tenant.getEmail());

        link.setLastSentDatetime(LocalDateTime.now());
        apartmentSharingLinkRepository.save(link);
    }

    private Document getDocumentManagedByTenant(Tenant tenant, Long documentId) {
        Document tenantSelectedDocument = tenant.getDocuments().stream().filter(document -> document.getId().equals(documentId)).findAny().orElse(null);
        if (tenantSelectedDocument != null) {
            return tenantSelectedDocument;
        }
        for (Guarantor guarantor : tenant.getGuarantors()) {
            Document guarantorSelectedDocument = guarantor.getDocuments().stream().filter(document -> document.getId().equals(documentId)).findAny().orElse(null);
            if (guarantorSelectedDocument != null) {
                return guarantorSelectedDocument;
            }
        }
        if (tenant.getApartmentSharing().getApplicationType().equals(ApplicationType.COUPLE) && tenant.getTenantType().equals(TenantType.CREATE)) {
            var couple = tenant.getApartmentSharing().getTenants().stream().filter(t -> !t.getId().equals(tenant.getId())).findAny();
            if (couple.isPresent()) {
                return getDocumentManagedByTenant(couple.get(), documentId);
            }
        }

        return null;
    }

    @Override
    public void addCommentAnalysis(Tenant tenant, Long documentId, String comment) {
        Document selectedDocument = getDocumentManagedByTenant(tenant, documentId);
        if (selectedDocument == null) {
            throw new NotFoundException();
        }
        DocumentAnalysisReport documentAnalysisReport = selectedDocument.getDocumentAnalysisReport();
        if (documentAnalysisReport == null) {
            throw new NotFoundException();
        }
        if (StringUtils.isBlank(comment)) {
            documentAnalysisReport.setComment(null);
        } else {
            documentAnalysisReport.setComment(comment);
        }
        documentAnalysisReportRepository.save(documentAnalysisReport);
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return tenantRepository.findByEmail(email);
    }
}
