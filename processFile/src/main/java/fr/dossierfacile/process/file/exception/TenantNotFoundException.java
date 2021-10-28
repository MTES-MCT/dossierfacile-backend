package fr.dossierfacile.process.file.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long id) {
        super("Could no find tenant with id " + id);
    }
}
