package fr.dossierfacile.api.pdf.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentTokenNotFoundException extends RuntimeException {
    public DocumentTokenNotFoundException(String token) {
        super("DocumentToken with token [" + token + "] NOT found in database");
    }
}
