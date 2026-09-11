package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Defensive safety net: the COMPLETED status must never reach the DTOs served to a
 * partner that did not opt in, nor to an owner (the automatic switch in registerTenant
 * and the opt-in eligibility rules are supposed to make this impossible). If the masking
 * ever triggers, an invariant is broken somewhere: the error log below is the alert to
 * investigate.
 * <p>
 * TODO(partner-completed-optin-100): once every partner has integrated the COMPLETED status, the partner
 * masking ({@link MasksCompletedStatusForPartner}) goes away. The owner masking
 * ({@link MasksCompletedStatusForOwner}) stays until the owner space handles COMPLETED,
 * then this class can be deleted too (grep "PartnerVisibleStatus.mask").
 */
@Slf4j
public final class PartnerVisibleStatus {

    private PartnerVisibleStatus() {
    }

    public static TenantFileStatus mask(TenantFileStatus status, String source) {
        if (status == TenantFileStatus.COMPLETED) {
            log.error("Defensive status masking triggered in {}: a COMPLETED dossier should never be exposed to a partner that did not opt in, nor to an owner", source);
            return TenantFileStatus.TO_PROCESS;
        }
        return status;
    }
}
