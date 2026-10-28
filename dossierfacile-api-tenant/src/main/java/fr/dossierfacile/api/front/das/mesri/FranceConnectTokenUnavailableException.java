package fr.dossierfacile.api.front.das.mesri;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The FranceConnect token is missing or expired: the frontend must trigger a new FranceConnect login.
 * 409 is used on purpose (not 401), because the frontend treats 401 as a lost session.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class FranceConnectTokenUnavailableException extends RuntimeException {
    public FranceConnectTokenUnavailableException(String message) {
        super(message);
    }
}
