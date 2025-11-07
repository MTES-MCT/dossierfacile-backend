package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TrigramNotAuthorizedException extends RuntimeException {

    public TrigramNotAuthorizedException(String message) {
        super(message);
    }
}

