package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletedEligibilityServiceImpl implements CompletedEligibilityService {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final TenantLogRepository tenantLogRepository;
    private final FeatureFlagService featureFlagService;

    @Override
    public boolean isEligibleForOptIn(Tenant tenant) {
        TenantFileStatus status = tenant.getStatus();
        if (status != TenantFileStatus.TO_PROCESS && status != TenantFileStatus.COMPLETED) {
            return false;
        }
        return checkEligibilityRules(tenant);
    }

    @Override
    public boolean canBeCompleted(Tenant tenant) {
        if (Boolean.TRUE.equals(tenant.getValidationRequested())) {
            return false;
        }
        return checkEligibilityRules(tenant);
    }

    private boolean checkEligibilityRules(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (apartmentSharing == null || apartmentSharing.getApplicationType() != ApplicationType.ALONE) {
            return false;
        }
        // Strict rule: any partner link (DFC or owner), even a dangling one, disables the opt-in
        if (tenantUserApiRepository.existsByTenant(tenant)) {
            return false;
        }
        if (tenantLogRepository.hasBeenValidatedOrDenied(tenant.getId())) {
            return false;
        }
        // The feature flag check comes last: its first evaluation persists a bucket
        // assignment for the user, so only real candidates enter the metrics denominator
        return featureFlagService.isFeatureEnabledForUser(tenant.getId(), COMPLETED_OPTIN_FEATURE_FLAG);
    }
}
