package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CoTenantEmailAlreadyExistsException extends RuntimeException {
    public CoTenantEmailAlreadyExistsException() {
        super("A co-tenant with this email already exists");
    }

    public CoTenantEmailAlreadyExistsException(String message) {
        super(message);
    }
}
