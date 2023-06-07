package fr.dossierfacile.api.pdfgenerator.exception;

import fr.dossierfacile.common.entity.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Document document) {
        super("Could not find document [ " + document.getId() + " , " + ((document.getDocumentCategory() == null) ? "-" : document.getDocumentCategory().name()) + "] ");
    }
}