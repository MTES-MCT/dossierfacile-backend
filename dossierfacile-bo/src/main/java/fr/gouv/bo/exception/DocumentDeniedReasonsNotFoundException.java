package fr.gouv.bo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentDeniedReasonsNotFoundException extends RuntimeException {
    public DocumentDeniedReasonsNotFoundException(Long id) {
        super("Could not find document_denied_reasons with ID " + id);
    }
}
