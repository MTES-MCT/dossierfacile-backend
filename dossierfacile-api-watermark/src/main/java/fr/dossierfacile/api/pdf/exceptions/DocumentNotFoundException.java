package fr.dossierfacile.api.pdf.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id, String categoryName) {
        super("Document [" + categoryName + "] with ID [" + id + "] NOT found in database");
    }
}
