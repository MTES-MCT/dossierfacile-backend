package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

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

    /**
     * Lists tenants that are flagged for auto-validation (ready_for_auto_validation = true),
     * in status TO_PROCESS, and modified before maxLastUpdateDate.
     */
    List<Tenant> listTenantsToAutoValidate(java.time.LocalDateTime maxLastUpdateDate);

    /**
     * Processes auto-validation for a single tenant in an isolated transaction.
     * Returns true if auto-validated, false if fallback to human operators.
     */
    boolean processAutoValidationForTenant(Long tenantId);

}
