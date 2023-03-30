package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TenantIllegalStateException extends RuntimeException {
    public TenantIllegalStateException(String msg) {
        super(msg);
    }
}