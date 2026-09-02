package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;

/**
 * Switch-back of a COMPLETED dossier to the operator queue, shared by every
 * switch-back path (partner link, BO rollback).
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
}
