package fr.dossierfacile.api.pdfgenerator.exception;

import fr.dossierfacile.common.enums.TenantType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String email) {
        super("Could no find tenant with email " + email);
    }

    public TenantNotFoundException(Long id) {
        super("Could no find tenant with id " + id);
    }

    public TenantNotFoundException(TenantType tenantType) {
        super("Could not find tenant with type " + tenantType.name());
    }
}
