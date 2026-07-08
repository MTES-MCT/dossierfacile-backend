package fr.dossierfacile.api.front.exception.model;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned by the tenant application endpoints
 * (notably POST /api/register/application/v2). They allow the frontend to map
 * a backend error to an explicit, accessible, localized message bound to the
 * relevant form field, instead of parsing technical English messages.
 */
public enum ApplicationErrorCode {
    /** A co-tenant email is already linked to an existing DossierFacile account. */
    CO_TENANT_EMAIL_ALREADY_EXISTS,
    /** A co-tenant email is missing while it is required (COUPLE / GROUP). */
    CO_TENANT_EMAIL_REQUIRED,
    /** The access/consent checkbox was not accepted. */
    ACCEPT_ACCESS_REQUIRED,
    /** A JOIN tenant is not allowed to change the application type. */
    APPLICATION_TYPE_DENIED_FOR_JOIN;

    public HttpStatus httpStatus() {
        return switch (this) {
            case CO_TENANT_EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case CO_TENANT_EMAIL_REQUIRED, ACCEPT_ACCESS_REQUIRED, APPLICATION_TYPE_DENIED_FOR_JOIN ->
                    HttpStatus.BAD_REQUEST;
        };
    }
}
