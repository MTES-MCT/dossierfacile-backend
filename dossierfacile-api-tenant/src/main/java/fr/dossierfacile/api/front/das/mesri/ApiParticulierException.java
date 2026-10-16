package fr.dossierfacile.api.front.das.mesri;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Unexpected answer or network failure from API Particulier. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ApiParticulierException extends RuntimeException {
    public ApiParticulierException(String message) {
        super(message);
    }
}
