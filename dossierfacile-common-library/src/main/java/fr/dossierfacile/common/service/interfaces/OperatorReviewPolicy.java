package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantFileStatus;

/**
 * Decides whether a complete dossier enters the operator queue (TO_PROCESS) or is
 * usable right away without operator review (COMPLETED).
 */
public interface OperatorReviewPolicy {

    String COMPLETED_OPTIN_FEATURE_FLAG = "tenant_completed_optin";
    /**
     * Global kill-switch for the partners flagged {@code user_api.completed_status_supported}.
     * Inactive: no partner accepts the COMPLETED status.
     * <p>
     * TODO(partner-completed-optin-100): drop the flag (and every {@code isPartnerOptedIn} call site) once every
     * partner has integrated COMPLETED.
     */
    String PARTNER_COMPLETED_OPTIN_FEATURE_FLAG = "partner_completed_optin";

    /**
     * Scope: the COMPLETED status exists for this dossier (ALONE, every linked partner
     * opted in, in the opt-in rollout).
     */
    boolean supportsCompletedStatus(Tenant tenant);

    /**
     * The partner accepts the COMPLETED status: flagged {@code completedStatusSupported} while
     * the {@link #PARTNER_COMPLETED_OPTIN_FEATURE_FLAG} flag is active. Such a partner sees COMPLETED in
     * its payloads, receives the COMPLETED_ACCOUNT webhook and does not send a COMPLETED
     * dossier back to the operator queue when linked. The owner partner is never opted in.
     */
    boolean isPartnerOptedIn(UserApi userApi);

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
