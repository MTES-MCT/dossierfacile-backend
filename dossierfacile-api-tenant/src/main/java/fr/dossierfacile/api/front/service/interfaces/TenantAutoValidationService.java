package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;

public interface TenantAutoValidationService {

    /**
     * Determines whether a document change is eligible for auto-validation flagging.
     * Currently true for Visale guarantee certificate documents.
     */
    boolean isEligibleForAutoValidation(Document document);

    /**
     * Determines whether a tenant is currently ready for auto-validation.
     * True if all non-auto-validatable documents are VALIDATED and all documents
     * currently TO_PROCESS are eligible for auto-validation.
     */
    boolean isTenantReadyForAutoValidation(Tenant tenant);

}
