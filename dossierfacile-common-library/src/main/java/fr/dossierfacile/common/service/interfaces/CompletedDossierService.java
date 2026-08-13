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
     * opt-in. Any other computed status is returned unchanged.
     */
    TenantFileStatus toCompletedIfEligible(Tenant tenant, TenantFileStatus computedStatus);

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
