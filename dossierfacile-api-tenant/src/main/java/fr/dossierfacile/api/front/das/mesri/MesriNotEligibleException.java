package fr.dossierfacile.api.front.das.mesri;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The tenant cannot use the feature: flag disabled, not FranceConnected, or not an ALONE application. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class MesriNotEligibleException extends RuntimeException {
    public MesriNotEligibleException(String message) {
        super(message);
    }
}
