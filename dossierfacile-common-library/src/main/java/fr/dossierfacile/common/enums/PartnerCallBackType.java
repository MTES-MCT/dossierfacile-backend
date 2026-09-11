package fr.dossierfacile.common.enums;

/**
 * Webhook event types sent to partners. Status events are named after the status the
 * dossier enters: CREATED_ACCOUNT (TO_PROCESS, in the operator queue),
 * COMPLETED_ACCOUNT (COMPLETED),
 * VERIFIED_ACCOUNT (VALIDATED), DENIED_ACCOUNT (DECLINED).
 */
public enum PartnerCallBackType {
    DELETED_ACCOUNT,
    VERIFIED_ACCOUNT,
    CREATED_ACCOUNT,
    DISSOCIATED_ACCOUNT,
    DENIED_ACCOUNT,
    ARCHIVED_ACCOUNT,
    RETURNED_ACCOUNT,
    MERGED_ACCOUNT,
    ACCESS_REVOKED,
    COMPLETED_ACCOUNT;

    /**
     * Event describing the current status of a dossier, used when a partner is (re)sent
     * the dossier as it stands: link to a partner, manual resend from the BO, scheduler.
     */
    public static PartnerCallBackType forTenantStatus(TenantFileStatus status) {
        if (status == TenantFileStatus.VALIDATED) {
            return VERIFIED_ACCOUNT;
        }
        if (status == TenantFileStatus.COMPLETED) {
            return COMPLETED_ACCOUNT;
        }
        return CREATED_ACCOUNT;
    }
}
