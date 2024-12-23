package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TenantWarningService {
    private final LogService logService;
    private final WarningMailSender mailSender;
    private final ConfirmationTokenService confirmationTokenService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final TenantCommonRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantCommonService tenantCommonService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final KeycloakCommonService keycloakCommonService;

    @Transactional
    public void handleTenantWarning(Tenant t, int warnings) {
        Optional<Tenant> optionalTenant = tenantRepository.findById(t.getId());
        if (optionalTenant.isEmpty()) {
            return;
        }
        Tenant tenant = optionalTenant.get();
        switch (warnings) {
            case 0 -> handleWarning0(tenant);
            case 1 -> handleWarning1(tenant);
            case 2 -> handleWarning2(tenant);
        }
    }

    private void handleWarning2(Tenant t) {
        log.info("accountWarnings. Documents deletion for tenant with ID [" + t.getId() + "]");

        logService.saveLogWithTenantData(LogType.ACCOUNT_ARCHIVED, t);
        tenantCommonService.deleteTenantData(t);
        t.getDocuments().clear();
        t.getGuarantors().clear();
        t.setWarnings(0);
        t.setConfirmationToken(null);
        t.setHonorDeclaration(false);
        t.setStatus(TenantFileStatus.ARCHIVED);
        t.setZipCode("");
        t.setAbroad(null);
        t.setClarification("");
        t.setLastUpdateDate(LocalDateTime.now());
        t = tenantRepository.save(t);

        logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, t.getId());
        partnerCallBackService.sendCallBack(t, PartnerCallBackType.ARCHIVED_ACCOUNT);

        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByUser(t);
        confirmationToken.ifPresent(confirmationTokenRepository::delete);
    }

    private void handleWarning1(Tenant t) {
        mailSender.sendEmailSecondWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
        t.setWarnings(2);
        tenantRepository.save(t);
        logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
    }

    private void handleWarning0(Tenant t) {
        mailSender.sendEmailFirstWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
        t.setWarnings(1);
        tenantRepository.save(t);
        logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
    }

    @Transactional
    public void deleteOldArchivedWarning(long tenantId) {
        log.info("Deleting tenant " + tenantId);
        Optional<Tenant> optionalTenant = tenantRepository.findById(tenantId);
        if (optionalTenant.isEmpty()) {
            return;
        }
        Tenant tenant = optionalTenant.get();
        logService.saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);

        Optional<ApartmentSharing> optionalApartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        ApartmentSharing apartmentSharing;
        if (optionalApartmentSharing.isPresent()) {
            apartmentSharing = optionalApartmentSharing.get();
        } else {
            return;
        }
        if (tenant.getTenantType() == TenantType.CREATE) {
            keycloakCommonService.deleteKeycloakUsers(
                    apartmentSharing.getTenants().stream().map(t -> (User) t).collect(Collectors.toList()));
            apartmentSharingCommonService.delete(apartmentSharing);
        } else {
            keycloakCommonService.deleteKeycloakUser(tenant);
            tenantRepository.delete(tenant);
            apartmentSharingCommonService.removeTenant(apartmentSharing, tenant);
        }
    }

}
