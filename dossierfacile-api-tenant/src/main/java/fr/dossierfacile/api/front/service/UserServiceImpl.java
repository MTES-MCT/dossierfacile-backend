package fr.dossierfacile.api.front.service;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.repository.AccountDeleteLogRepository;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.AccountDeleteLog;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.OvhService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final MailService mailService;
    private final PasswordRecoveryTokenService passwordRecoveryTokenService;
    private final OvhService ovhService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final AccountDeleteLogRepository accountDeleteLogRepository;
    private final TenantMapper tenantMapper;
    private final TenantCommonRepository tenantRepository;
    private final LogService logService;
    private final Gson gson = new Gson();
    private final KeycloakService keycloakService;
    private final SourceService sourceService;
    private final PartnerCallBackService partnerCallBackService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public long confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ConfirmationTokenNotFoundException(token));
        User user = confirmationToken.getUser();
        user.setEnabled(true);
        user.setConfirmationToken(null);
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        confirmationTokenRepository.delete(confirmationToken);
        keycloakService.confirmKeycloakUser(user.getKeycloakId());
        tenantRepository.resetWarnings(user.getId());
        return user.getId();
    }

    @Override
    public TenantModel createPassword(String token, String password) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordRecoveryTokenNotFoundException(token));
        User user = passwordRecoveryToken.getUser();
        user.setEnabled(true);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        if (user.getKeycloakId() == null) {
            var keycloakId = keycloakService.getKeycloakId(user.getEmail());
            keycloakService.createKeyCloakPassword(keycloakId, password);
            user.setKeycloakId(keycloakId);
        } else {
            keycloakService.createKeyCloakPassword(user.getKeycloakId(), password);
        }
        userRepository.save(user);

        passwordRecoveryTokenRepository.delete(passwordRecoveryToken);
        return tenantMapper.toTenantModel(tenantRepository.getOne(user.getId()));
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.create(user);
        mailService.sendEmailNewPassword(user, passwordRecoveryToken);
    }

    @Override
    public void deleteAccount(Tenant tenant) {
        saveAndDeleteInfoByTenant(tenant);
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (tenant.getTenantType() == TenantType.CREATE || apartmentSharing.getNumberOfTenants() == 1) {
            log.info("Removing apartment_sharing with id [" + apartmentSharing.getId() + "] with [" + apartmentSharing.getNumberOfTenants() + "] tenants");
            keycloakService.deleteKeycloakUsers(apartmentSharing.getTenants());
            apartmentSharingRepository.delete(apartmentSharing);
        } else {
            log.info("Removing user/tenant with id [" + tenant.getId() + "]");
            logService.saveLog(LogType.ACCOUNT_DELETE, tenant.getId());
            keycloakService.deleteKeycloakUser(tenant);
            userRepository.delete(tenant);
        }
    }

    @Override
    public Boolean deleteCoTenant(Tenant tenant, Long id) {
        if (tenant.getTenantType().equals(TenantType.CREATE)) {
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Tenant coTenant = apartmentSharing.getTenants().stream().filter(t -> t.getId().equals(id) && t.getTenantType().equals(TenantType.JOIN)).findFirst().orElseThrow(null);
            if (coTenant != null) {
                if (coTenant.getKeycloakId() != null) {
                    keycloakService.deleteKeycloakUser(coTenant);
                }
                saveAndDeleteInfoByTenant(coTenant);
                userRepository.delete(coTenant);
                apartmentSharing.getTenants().remove(coTenant);
                updateApplicationTypeOfApartmentAfterDeletionOfCotenant(apartmentSharing);
                apartmentSharingService.resetDossierPdfGenerated(apartmentSharing);
                logService.saveLog(LogType.ACCOUNT_DELETE, id);
                return true;
            }
        }
        return false;
    }

    @Override
    public void linkTenantToPartner(Tenant tenant, PartnerForm partnerForm) {
        if (partnerForm.getSource() != null) {
            UserApi userApi = sourceService.findOrCreate(partnerForm.getSource());
            partnerCallBackService.registerTenant(partnerForm.getInternalPartnerId(), tenant, userApi);
        }
    }

    @Override
    public void linkTenantToPartner(Tenant tenant, String partner) {
        sourceService.findByName(partner).ifPresent(userApi -> partnerCallBackService.registerTenant(null, tenant, userApi));
    }

    @Override
    public void logout(Tenant tenant) {
        keycloakService.logout(tenant);
    }

    private void saveAndDeleteInfoByTenant(Tenant tenant) {
        mailService.sendEmailAccountDeleted(tenant);
        this.savingJsonProfileBeforeDeletion(tenantMapper.toTenantModel(tenant));

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(this::deleteFilesFromStorage);
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(this::deleteFilesFromStorage)
                );
    }

    private void savingJsonProfileBeforeDeletion(TenantModel tenantModel) {
        accountDeleteLogRepository.save(
                AccountDeleteLog.builder()
                        .userId(tenantModel.getId())
                        .deletionDate(LocalDateTime.now())
                        .jsonProfileBeforeDeletion(gson.toJson(tenantModel))
                        .build()
        );
    }

    private void deleteFilesFromStorage(Document document) {
        List<File> files = document.getFiles();
        if (files != null && !files.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            ovhService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            ovhService.delete(document.getName());
        }
    }

    private void updateApplicationTypeOfApartmentAfterDeletionOfCotenant(ApartmentSharing apartmentSharing) {
        ApplicationType nextApplicationType = ApplicationType.ALONE;

        //Current application can only be in this point [COUPLE] or [GROUP]
        ApplicationType previousApplicationType = apartmentSharing.getApplicationType();

        //If previous application was a [GROUP] and after deletion of 1 cotenant, it has now (>=2) tenants then,
        // it will stay as an application [GROUP]. Otherwise it will become an application [ALONE]
        if (previousApplicationType == ApplicationType.GROUP
                && apartmentSharing.getNumberOfTenants() >= 2) {
            nextApplicationType = ApplicationType.GROUP;
        }

        if (previousApplicationType != nextApplicationType) {
            log.info("Changing applicationType of apartment with ID [" + apartmentSharing.getId() + "] from [" + previousApplicationType.name() + "] to [" + nextApplicationType.name() + "]");
            apartmentSharing.setApplicationType(nextApplicationType);
            apartmentSharingRepository.save(apartmentSharing);
        }
    }
}
