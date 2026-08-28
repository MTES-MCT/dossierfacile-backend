package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletedEligibilityServiceImpl implements CompletedEligibilityService {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final FeatureFlagService featureFlagService;
    private final LotteryTicketRepository lotteryTicketRepository;

    @Override
    public boolean isEligibleForOptIn(Tenant tenant) {
        TenantFileStatus status = tenant.getStatus();
        // The question is available on any submitted dossier. On a VALIDATED or
        // DECLINED dossier the choice has no immediate effect on the status: it
        // applies to the next re-submission
        if (status != TenantFileStatus.TO_PROCESS && status != TenantFileStatus.COMPLETED
                && status != TenantFileStatus.VALIDATED && status != TenantFileStatus.DECLINED) {
            return false;
        }
        return checkEligibilityRules(tenant);
    }

    @Override
    public boolean canBeCompleted(Tenant tenant) {
        if (featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)) {
            // Lottery mode: only a DRAWN ticket keeps the dossier in the
            // queue; validationRequested is purely declarative
            if (lotteryTicketRepository.findFirstByTenantIdAndStatusIn(
                    tenant.getId(), Set.of(LotteryTicketStatus.DRAWN)).isPresent()) {
                return false;
            }
        } else if (Boolean.TRUE.equals(tenant.getValidationRequested())) {
            return false;
        }
        return checkEligibilityRules(tenant);
    }

    private boolean checkEligibilityRules(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        // TODO(completed-optin): relax this rule once we have defined how to handle COMPLETED dossier for couple and roommates
        if (apartmentSharing == null || !ApplicationType.ALONE.equals(apartmentSharing.getApplicationType())) {
            return false;
        }
        // Strict rule: any partner link (DFC or owner), even a dangling one, disables the opt-in
        // TODO(completed-optin): relax this rule once partners handle the COMPLETED status
        if (tenantUserApiRepository.existsByTenant(tenant)) {
            return false;
        }
        // The feature flag check comes last: its first evaluation persists a bucket
        // assignment for the user, so only real candidates enter the metrics denominator
        return featureFlagService.isFeatureEnabledForUser(tenant.getId(), COMPLETED_OPTIN_FEATURE_FLAG);
    }
}
