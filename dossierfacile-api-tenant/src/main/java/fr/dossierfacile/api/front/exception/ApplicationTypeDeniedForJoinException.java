package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ApplicationTypeDeniedForJoinException extends RuntimeException {
    public ApplicationTypeDeniedForJoinException() {
        super("A JOIN tenant is not allowed to change the application type");
    }
}
