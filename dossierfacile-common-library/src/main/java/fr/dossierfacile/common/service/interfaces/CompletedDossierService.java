package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;

/**
 * Switch-back of a COMPLETED dossier to the operator queue, shared by every
 * switch-back path (partner link, BO rollback, BO support action).
 */
public interface CompletedDossierService {

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

    /**
     * BO support action: sends a COMPLETED dossier to the operator queue on behalf
     * of the tenant. Same switch as {@link #switchBackToProcessing}, plus
     * validationRequested = true so that the dossier stays in the queue across
     * status recomputations. No mail. No-op when the dossier is not COMPLETED.
     *
     * @param operatorId the support operator, recorded on the tenant log
     */
    void forceOperatorReview(Tenant tenant, Long operatorId);
}
