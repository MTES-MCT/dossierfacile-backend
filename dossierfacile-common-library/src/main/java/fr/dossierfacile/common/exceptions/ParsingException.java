package fr.dossierfacile.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ParsingException extends RuntimeException {

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
