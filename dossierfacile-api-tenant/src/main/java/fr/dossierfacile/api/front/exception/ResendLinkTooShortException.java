package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class ResendLinkTooShortException extends RuntimeException {
    public ResendLinkTooShortException() {
        super("Delay between two resend is too short");
    }
}
