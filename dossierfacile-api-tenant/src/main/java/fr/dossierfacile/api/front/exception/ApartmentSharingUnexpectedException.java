package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.EXPECTATION_FAILED)
public class ApartmentSharingUnexpectedException extends RuntimeException {
    public ApartmentSharingUnexpectedException(String token) {
        super("Some tenants are not yet validated or have null pdf documents in apartment sharing with token [" + token + "]");
    }

    public ApartmentSharingUnexpectedException() {
        super("Some tenants are not yet validated or have null pdf documents in apartment sharing");
    }
}
