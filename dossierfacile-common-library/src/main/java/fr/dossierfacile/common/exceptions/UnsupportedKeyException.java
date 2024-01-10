package fr.dossierfacile.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedKeyException extends RuntimeException {

    public UnsupportedKeyException(String message) {
        super(message);
    }

}
