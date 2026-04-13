package fr.gouv.bo.security;

public interface BOApplicationAccessService {

    /**
     * Checks that the principal is authorised to view a dossier identified by tenantId.
     * For OPERATOR: requires a START_PROCESS or STOP_PROCESS log for that tenant in the last 24 hours.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Throws AccessDeniedException if an OPERATOR is not assigned.
     */
    void checkTenantAccess(UserPrincipal principal, Long tenantId);

    /**
     * Checks that the principal is authorised to view a dossier identified by apartmentSharingId.
     * For OPERATOR: requires a START_PROCESS or STOP_PROCESS log for any tenant of that apartment sharing in the last 24 hours.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Always writes a VIEW_APPLICATION entry to operator_log anchored to the CREATE tenant.
     * Throws AccessDeniedException if an OPERATOR is not assigned.
     */
    void checkAndLogApartmentSharingAccess(UserPrincipal principal, Long apartmentSharingId);

    /**
     * Logs a tenant search action performed from BO search endpoints.
     */
    void logSearchTenant(UserPrincipal principal, String query, long resultCount);
}
