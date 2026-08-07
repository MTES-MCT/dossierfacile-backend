package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;

/**
 * Eligibility rules for the COMPLETED opt-in MVP: a submitted ALONE dossier can get
 * the COMPLETED status (usable immediately, no operator verification) instead of TO_PROCESS.
 * Eligibility is always computed on the fly, never stored: only the user's explicit
 * choice ({@code tenant.validationRequested}) is persisted.
 */
public interface CompletedEligibilityService {

    String COMPLETED_OPTIN_FEATURE_FLAG = "tenant_completed_optin";

    /**
     * Whether the opt-in choice is available to this tenant (drives the dashboard widget).
     * Requires a submitted dossier (TO_PROCESS or COMPLETED), ALONE application, no partner
     * link, never validated nor denied, and the feature flag enabled for this user.
     * Does NOT depend on {@code validationRequested}: the widget stays visible after an answer.
     */
    boolean isEligibleForOptIn(Tenant tenant);

    /**
     * Whether a dossier whose computed status is TO_PROCESS must get the COMPLETED status instead.
     * Caller guarantees the dossier is complete and submitted (computed status TO_PROCESS).
     */
    boolean canBeCompleted(Tenant tenant);
}
