package fr.dossierfacile.api.front.domain.service;

import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSynchronizationDomainService {

    private final TenantCommonRepository tenantRepository;
    private final TenantStatusService tenantStatusService;
    private final LogService logService;
    private final DocumentService documentService;

    @Transactional
    public Tenant synchronizeTenant(Tenant tenant, KeycloakUser user) {
        if (matches(tenant, user)) {
            return tenant;
        }
        if (!Strings.CI.equals(tenant.getEmail(), user.getEmail())) {
            log.error("Tenant email and current logged email mismatch FC? '%s' vs '%s' - tenant won't be synchronized".formatted(tenant.getEmail(), user.getEmail()));
            tenant.setWarningMessage("Attention, l'email de compte est '%s' et l'email de connexion est '%s'".formatted(tenant.getEmail(), user.getEmail()));
            return tenant;
        }

        boolean userHasBeenLinkedToFranceConnect = processAccountLinking(tenant, user);

        tenant.setKeycloakId(user.getKeycloakId());
        tenant.setFranceConnect(user.isFranceConnect());
        tenant.setFranceConnectSub(user.getFranceConnectSub());
        tenant.setFranceConnectBirthCountry(user.getFranceConnectBirthCountry());
        tenant.setFranceConnectBirthPlace(user.getFranceConnectBirthPlace());
        tenant.setFranceConnectBirthDate(user.getFranceConnectBirthDate());
        tenant.setFcHash(user.isFranceConnect() ? user.getFcHash() : null);

        updateTenantProfile(tenant, user, userHasBeenLinkedToFranceConnect);

        tenant.lastUpdateDateProfile(LocalDateTime.now(ZoneId.systemDefault()), null);
        tenantStatusService.updateTenantStatus(tenant);

        return tenantRepository.saveAndFlush(tenant);
    }

    private boolean processAccountLinking(Tenant tenant, KeycloakUser user) {
        boolean userHasBeenLinkedToFranceConnect = false;
        if (!Boolean.TRUE.equals(tenant.getFranceConnect()) && user.isFranceConnect()) {
            log.info("Local account link to FranceConnect account, for tenant with ID {}", tenant.getId());
            logService.saveLog(LogType.FC_ACCOUNT_LINK, tenant.getId());
            userHasBeenLinkedToFranceConnect = true;
        } else if (tenant.getKeycloakId() == null) {
            log.info("First tenant connection from DF, for tenant with ID {}", tenant.getId());
            logService.saveLog(LogType.ACCOUNT_LINK, tenant.getId());
        }
        return userHasBeenLinkedToFranceConnect;
    }

    private void updateTenantProfile(Tenant tenant, KeycloakUser user, boolean userHasBeenLinkedToFranceConnect) {
        if (userHasBeenLinkedToFranceConnect || (user.isFranceConnect() && tenant.getOwnerType() == TenantOwnerType.SELF)) {
            if (shouldResetDocuments(tenant, user)) {
                documentService.resetValidatedOrInProgressDocumentsAccordingCategories(tenant.getDocuments(),
                        Arrays.asList(DocumentCategory.values()));
            }
            tenant.setUserFirstName(user.getGivenName());
            tenant.setUserLastName(user.getFamilyName());
            tenant.setUserPreferredName(user.getComputedPreferredUserName() == null ? tenant.getUserPreferredName() : user.getComputedPreferredUserName());
        }
    }

    private boolean shouldResetDocuments(Tenant tenant, KeycloakUser user) {
        return !Strings.CS.equals(tenant.getFirstName(), user.getGivenName())
                || !Strings.CS.equals(tenant.getLastName(), user.getFamilyName())
                || (user.getComputedPreferredUserName() != null && !Strings.CS.equals(tenant.getPreferredName(), user.getComputedPreferredUserName()));
    }

    public boolean matches(Tenant tenant, KeycloakUser user) {
        return Strings.CS.equals(tenant.getKeycloakId(), user.getKeycloakId())
                && Strings.CS.equals(tenant.getEmail(), user.getEmail())
                && tenant.getFranceConnect() == user.isFranceConnect()
                && (!user.isFranceConnect() ||
                (Strings.CI.equals(tenant.getUserFirstName(), user.getGivenName()) ||
                        Strings.CI.equals(tenant.getUserLastName(), user.getFamilyName())
                ))
                && (!user.isFranceConnect() || java.util.Objects.equals(tenant.getFcHash(), user.getFcHash()));
    }
}
