package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserApiNotFoundException extends RuntimeException {

    public UserApiNotFoundException(Long id) {
        super("Could not find user_api with id " + id);
    }
}
