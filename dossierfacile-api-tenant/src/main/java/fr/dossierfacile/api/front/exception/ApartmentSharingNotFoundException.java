package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApartmentSharingNotFoundException extends RuntimeException {
    public ApartmentSharingNotFoundException(String token) {
        super("Could not find apartment sharing with token " + token);
    }
}
