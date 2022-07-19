package fr.dossierfacile.api.pdf.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PROCESSING)
public class InProgressException extends RuntimeException {
    public InProgressException(String message) {
        super(message);
    }
}
