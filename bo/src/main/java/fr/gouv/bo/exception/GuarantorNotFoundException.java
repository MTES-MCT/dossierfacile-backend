package fr.gouv.bo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GuarantorNotFoundException extends RuntimeException {
    public GuarantorNotFoundException(Long id) {
        super("Could not find guarantor with id " + id);
    }
}
