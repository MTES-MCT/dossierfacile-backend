package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;

public interface TenantAutoValidationService {

    /**
     * Determines whether a document change is eligible for auto-validation flagging.
     * Currently true for Visale guarantee certificate documents.
     */
    boolean isEligibleForAutoValidation(Document document);

}
