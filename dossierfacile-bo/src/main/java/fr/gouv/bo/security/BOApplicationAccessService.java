package fr.gouv.bo.security;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;

public interface BOApplicationAccessService {

    /**
     * Checks that the principal is authorised to view a dossier identified by tenantId.
     * For OPERATOR: requires a START_PROCESS or STOP_PROCESS log for that tenant in the last 24 hours.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Throws AccessDeniedException if an OPERATOR is not assigned.
     * No quota is enforced by this method (quota is checked at START_PROCESS time).
     */
    void checkTenantAccess(UserPrincipal principal, Long tenantId);

    /**
     * Checks that the principal is authorised to access a dossier identified by apartmentSharingId.
     * For OPERATOR: requires a START_PROCESS or STOP_PROCESS log for any tenant of that apartment sharing
     * in the configured assignment window.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Throws AccessDeniedException with a generic message if an OPERATOR is not assigned.
     */
    void checkApartmentSharingAccess(UserPrincipal principal, Long apartmentSharingId);

    /**
     * Checks that the principal is authorised to view a dossier identified by apartmentSharingId.
     * For OPERATOR: requires a START_PROCESS or STOP_PROCESS log for any tenant of that apartment sharing in the last 24 hours.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Enforces the daily VIEW_APPLICATION quota (limit for all roles).
     * Always writes a VIEW_APPLICATION entry to operator_log anchored to the CREATE tenant.
     * Throws AccessDeniedException if an OPERATOR is not assigned or the daily quota is reached.
     */
    void checkAndLogApartmentSharingAccess(UserPrincipal principal, Long apartmentSharingId);

    /**
     * Enforces the daily SEARCH_TENANT quota (limit for SUPPORT / MANAGER / ADMIN),
     * then logs the search action in operator_log.
     * Throws AccessDeniedException if the daily quota is reached.
     */
    void checkAndLogSearchTenant(UserPrincipal principal, String query, long resultCount);

    /**
     * Checks that the principal is authorised to access the dossier owning the given file.
     * For OPERATOR: requires assignment to the resolved tenant in the configured window.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Throws AccessDeniedException if the file/document/tenant chain is broken or operator is not assigned.
     */
    void checkFileAccess(UserPrincipal principal, File file);

    /**
     * Checks that the principal is authorised to access the dossier owning the given document.
     * For OPERATOR: requires assignment to the resolved tenant in the configured window.
     * For SUPPORT / MANAGER / ADMIN: always authorised.
     * Throws AccessDeniedException if the document/tenant chain is broken or operator is not assigned.
     */
    void checkDocumentAccess(UserPrincipal principal, Document document);
}
