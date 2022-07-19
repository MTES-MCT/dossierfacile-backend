package fr.dossierfacile.api.pdf.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DocumentBadRequestException extends RuntimeException {
    public DocumentBadRequestException(String message) {
        super(message);
    }
}
