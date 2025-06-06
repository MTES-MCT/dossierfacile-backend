package fr.dossierfacile.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class HttpBadGatewayException extends RuntimeException {
    public HttpBadGatewayException(String message) {
        super(message);
    }
}
