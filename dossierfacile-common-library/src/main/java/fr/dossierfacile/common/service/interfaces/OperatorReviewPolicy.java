package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;

/**
 * Decides whether a complete dossier enters the operator queue (TO_PROCESS) or is
 * usable right away without operator review (COMPLETED).
 */
public interface OperatorReviewPolicy {

    String COMPLETED_OPTIN_FEATURE_FLAG = "tenant_completed_optin";

    /**
     * Scope: the COMPLETED status exists for this dossier (ALONE, no partner link, in the
     * opt-in rollout).
     */
    boolean supportsCompletedStatus(Tenant tenant);

    /**
     * The "request an operator review" choice is available: dossier
     * {@link TenantFileStatus#isCompletedOrBetter()} or DECLINED, and in the scope.
     * On VALIDATED / DECLINED the choice applies to the next re-submission.
     */
    boolean canRequestOperatorReview(Tenant tenant);

    /**
     * Queue state: a slot in the operator queue has been granted.
     */
    boolean isOperatorReviewGranted(Tenant tenant);

    /**
     * Status to persist: a computed TO_PROCESS becomes COMPLETED when the dossier is
     * in the scope and no review has been granted. Any other computed status is returned unchanged.
     */
    TenantFileStatus resolveStatus(Tenant tenant, TenantFileStatus computedStatus);
}
