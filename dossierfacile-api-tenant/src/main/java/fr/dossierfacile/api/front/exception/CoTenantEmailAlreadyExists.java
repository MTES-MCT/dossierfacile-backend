package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CoTenantEmailAlreadyExists extends RuntimeException {
    public CoTenantEmailAlreadyExists() {
        super("A co-tenant with this email already exists");
    }
}