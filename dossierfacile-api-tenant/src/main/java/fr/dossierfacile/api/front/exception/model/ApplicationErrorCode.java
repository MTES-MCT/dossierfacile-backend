package fr.dossierfacile.api.front.exception.model;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned by the tenant application endpoints
 * (notably POST /api/register/application/v2). They allow the frontend to display specific error messages.   
 */
public enum ApplicationErrorCode {
    /** A co-tenant email is already linked to an existing DossierFacile account. */
    CO_TENANT_EMAIL_ALREADY_EXISTS,
    /** A JOIN tenant is not allowed to change the application type. */
    APPLICATION_TYPE_DENIED_FOR_JOIN;

    public HttpStatus httpStatus() {
        return switch (this) {
            case CO_TENANT_EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case APPLICATION_TYPE_DENIED_FOR_JOIN -> HttpStatus.BAD_REQUEST;
        };
    }
}
