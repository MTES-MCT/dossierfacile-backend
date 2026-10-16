package fr.dossierfacile.api.front.das.mesri;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The student exists but API Particulier returned no admission at all: nothing to attest. */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MesriNoAdmissionException extends RuntimeException {
    public MesriNoAdmissionException() {
        super("API Particulier returned no admission for this student");
    }
}
