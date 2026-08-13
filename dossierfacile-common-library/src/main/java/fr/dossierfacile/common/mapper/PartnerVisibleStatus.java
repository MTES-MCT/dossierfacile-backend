package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Defensive safety net: the COMPLETED status must never reach a partner or owner
 * facing DTO (the automatic switch in registerTenant and the opt-in eligibility rules
 * are supposed to make this impossible). If the masking ever triggers, an invariant
 * is broken somewhere: the error log below is the alert to investigate.
 * <p>
 * TODO(completed-optin): once partners handle the COMPLETED status, remove this class
 * and its call sites in the partner and owner mappers (grep "PartnerVisibleStatus.mask").
 */
@Slf4j
public final class PartnerVisibleStatus {

    private PartnerVisibleStatus() {
    }

    public static TenantFileStatus mask(TenantFileStatus status, String source) {
        if (status == TenantFileStatus.COMPLETED) {
            log.error("Defensive status masking triggered in {}: a COMPLETED dossier should never be exposed to partners or owners", source);
            return TenantFileStatus.TO_PROCESS;
        }
        return status;
    }
}
