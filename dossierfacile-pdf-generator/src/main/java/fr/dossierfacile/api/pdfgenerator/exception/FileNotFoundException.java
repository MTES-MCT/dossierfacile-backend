package fr.dossierfacile.api.pdfgenerator.exception;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(Long id) {
        super("Could not find file with id " + id);
    }

    public FileNotFoundException(Long fileId, Tenant tenant) {
        super("Could not find file with id " + fileId + " associated to tenant with id " + tenant.getId());
    }
}
