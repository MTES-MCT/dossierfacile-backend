package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantUserApiNotFoundException extends RuntimeException {

    public TenantUserApiNotFoundException(Long tenantId, String keycloakClientId) {
        super("Could not find tenant with ID [" + tenantId + "] linked to the keycloak client [" + keycloakClientId + "]");
    }
}
