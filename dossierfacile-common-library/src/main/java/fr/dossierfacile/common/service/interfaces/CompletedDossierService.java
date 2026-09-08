package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantFileStatus;

/**
 * Shared transitions of the COMPLETED opt-in MVP, used by both status update
 * implementations (api-tenant and BO) and by every switch-back path.
 */
public interface CompletedDossierService {

    /**
     * Returns COMPLETED instead of TO_PROCESS when the dossier is eligible to the
     * opt-in and is ENTERING the operator queue (its persisted status is not
     * TO_PROCESS). Any other computed status is returned unchanged.
     * <p>
     * Invariant: a dossier already waiting in the queue never leaves it through a
     * status recomputation (tenant edit, operator edit in the BO, rollout increase
     * of the feature flag). Leaving the queue is always an explicit decision: the
     * operator verdict, or the tenant's opt-out via {@link #switchToCompleted}.
     */
    TenantFileStatus toCompletedIfEligible(Tenant tenant, TenantFileStatus computedStatus);

    /**
     * Explicit opt-out: takes a TO_PROCESS dossier out of the operator queue and
     * makes it COMPLETED. No-op (returns false) when the dossier is not TO_PROCESS
     * or not eligible. The caller is responsible for having persisted the user's
     * choice (validationRequested) beforehand, as eligibility depends on it.
     */
    boolean switchToCompleted(Tenant tenant);

    /**
     * Sends a COMPLETED dossier back to the operator queue: status TO_PROCESS,
     * queue position at switch time, tenant log and mail. No-op when the dossier
     * is not COMPLETED. The user's explicit choice (validationRequested) is left
     * untouched. Runs in its own transaction when none is active (rollback batch).
     *
     * @param userApi the partner whose linking triggered the switch; the mail
     *                template mentions it, so no mail is sent when null (BO rollback)
     */
    void switchBackToProcessing(Tenant tenant, UserApi userApi);
}
