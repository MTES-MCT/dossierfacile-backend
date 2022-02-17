package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConfirmationTokenNotFoundException extends RuntimeException {
    public ConfirmationTokenNotFoundException(String token) {
        super("Could not find confirmation token " + token);
    }
    public ConfirmationTokenNotFoundException(long userId) {
        super("Could not find confirmation token for user with ID [" + userId + "]");
    }
}
