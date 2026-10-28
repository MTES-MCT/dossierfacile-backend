package fr.dossierfacile.api.front.das.mesri;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** API Particulier found no student for the FranceConnect identity (code 25003). */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MesriStudentNotFoundException extends RuntimeException {
    public MesriStudentNotFoundException(String message) {
        super(message);
    }
}
