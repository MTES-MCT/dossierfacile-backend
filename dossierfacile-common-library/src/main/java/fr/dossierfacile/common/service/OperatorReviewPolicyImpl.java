package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorReviewPolicyImpl implements OperatorReviewPolicy {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final FeatureFlagService featureFlagService;
    private final LotteryTicketRepository lotteryTicketRepository;

    @Override
    public boolean supportsCompletedStatus(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        // TODO(completed-optin): relax this rule once we have defined how to handle COMPLETED dossier for couple and roommates
        if (apartmentSharing == null || !ApplicationType.ALONE.equals(apartmentSharing.getApplicationType())) {
            return false;
        }
        // Every linked partner (DFC or owner), even a dangling link, must have opted in to the
        // COMPLETED status: a single partner that did not opt in forces the operator queue
        // TODO(partner-completed-optin-100): drop this rule once every partner has integrated COMPLETED
        // (keep a check on the owner partner as long as the owner space is excluded)
        boolean everyLinkedPartnerOptedIn = tenantUserApiRepository.findAllByTenant(tenant).stream()
                .allMatch(link -> isPartnerOptedIn(link.getUserApi()));
        if (!everyLinkedPartnerOptedIn) {
            return false;
        }
        // The feature flag check comes last: its first evaluation persists a bucket
        // assignment for the user, so only real candidates enter the metrics denominator
        // TODO(completed-optin-rollout-100): remove this check (and the flag) once the rollout
        //  is stable at 100%.
        return featureFlagService.isFeatureEnabledForUser(tenant.getId(), COMPLETED_OPTIN_FEATURE_FLAG);
    }

    // TODO(partner-completed-optin-100): remove with the partner_completed_optin flag once every partner has integrated COMPLETED
    @Override
    public boolean isPartnerOptedIn(UserApi userApi) {
        return featureFlagService.isPartnerOptedIn(PARTNER_COMPLETED_OPTIN_FEATURE_FLAG, userApi);
    }

    @Override
    public boolean canRequestOperatorReview(Tenant tenant) {
        TenantFileStatus status = tenant.getStatus();
        // A DECLINED dossier keeps the choice: it applies to the next re-submission
        if (status == null || !(status.isCompletedOrBetter() || status == TenantFileStatus.DECLINED)) {
            return false;
        }
        return supportsCompletedStatus(tenant);
    }

    @Override
    public boolean isOperatorReviewGranted(Tenant tenant) {
        if (featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)) {
            return lotteryTicketRepository.findFirstByTenantIdAndStatusIn(
                    tenant.getId(), Set.of(LotteryTicketStatus.DRAWN)).isPresent();
        }
        return Boolean.TRUE.equals(tenant.getValidationRequested());
    }

    @Override
    public TenantFileStatus resolveStatus(Tenant tenant, TenantFileStatus computedStatus) {
        if (computedStatus != TenantFileStatus.TO_PROCESS) {
            return computedStatus;
        }
        // Queue state first: skips the flag evaluation (bucket assignment) for dossiers already in the queue
        if (isOperatorReviewGranted(tenant) || !supportsCompletedStatus(tenant)) {
            return TenantFileStatus.TO_PROCESS;
        }
        return TenantFileStatus.COMPLETED;
    }
}
