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
public class TenantArchivingService {

    private final LogService logService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final TenantCommonRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantCommonService tenantCommonService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final KeycloakCommonService keycloakCommonService;

    /**
     * Archives a tenant: deletes all documents and resets the tenant entity to an archived state.
     * Notifies partners and cleans up any pending confirmation token.
     */
    @Transactional
    public void archiveTenant(Tenant t) {
        Tenant tenant = tenantRepository.findById(t.getId()).orElse(null);
        if (tenant == null) {
            return;
        }
        log.info("Archiving tenant [{}]", tenant.getId());
        logService.saveLogWithTenantData(LogType.ACCOUNT_ARCHIVED, tenant);

        deleteDocuments(tenant);
        resetToArchivedStatus(tenant);

        tenant = tenantRepository.save(tenant);
        logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, tenant.getId());
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.ARCHIVED_ACCOUNT);

        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByUser(tenant);
        confirmationToken.ifPresent(confirmationTokenRepository::delete);
    }

    /**
     * Permanently deletes an archived tenant account from the database and from Keycloak.
     * If the tenant is the main tenant (CREATE), the whole apartment sharing is deleted.
     * If the tenant is a co-tenant (JOIN), only their own record is removed.
     */
    @Transactional
    public void deletePermanently(long tenantId) {
        log.info("Permanently deleting tenant [{}]", tenantId);
        Optional<Tenant> optionalTenant = tenantRepository.findById(tenantId);
        if (optionalTenant.isEmpty()) {
            return;
        }
        Tenant tenant = optionalTenant.get();
        logService.saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);

        Optional<ApartmentSharing> optionalApartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (optionalApartmentSharing.isEmpty()) {
            return;
        }
        ApartmentSharing apartmentSharing = optionalApartmentSharing.get();

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

    /**
     * Deletes all documents and guarantor documents for a tenant via TenantCommonService,
     * then clears the in-memory collections.
     */
    private void deleteDocuments(Tenant tenant) {
        tenantCommonService.deleteTenantData(tenant);
        tenant.getDocuments().clear();
        tenant.getGuarantors().clear();
    }

    /**
     * Resets all tenant profile fields to their archived state.
     * Does not persist — caller is responsible for saving the entity.
     */
    private void resetToArchivedStatus(Tenant tenant) {
        tenant.setStatus(TenantFileStatus.ARCHIVED);
        tenant.setWarnings(0);
        tenant.setConfirmationToken(null);
        tenant.setHonorDeclaration(false);
        tenant.setZipCode("");
        tenant.setAbroad(null);
        tenant.setClarification("");
        tenant.setLastUpdateDate(LocalDateTime.now());
    }
}
