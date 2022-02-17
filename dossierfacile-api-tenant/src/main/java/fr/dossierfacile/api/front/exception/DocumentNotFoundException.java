package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("Could not find document with ID " + id);
    }
    public DocumentNotFoundException(String name) {
        super("Could not find document with name " + name);
    }
    public DocumentNotFoundException(String categoryName, Long id) {
        super("Document [" + categoryName + "] with ID [" + id + "] found in database but not found in the storage provider");
    }
    public DocumentNotFoundException(String categoryName, Long id, Long tenantId) {
        super("Could not find document with ID [" + id + "], category [" + categoryName + "] and associated to tenant with ID [" + tenantId + "]");
    }
}
