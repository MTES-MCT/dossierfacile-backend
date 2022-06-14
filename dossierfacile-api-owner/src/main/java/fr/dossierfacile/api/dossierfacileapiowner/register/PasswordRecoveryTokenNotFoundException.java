package fr.dossierfacile.api.dossierfacileapiowner.register;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PasswordRecoveryTokenNotFoundException extends RuntimeException {
    public PasswordRecoveryTokenNotFoundException(String token) {
        super("Could not find password recovery token or is expired " + token);
    }
}
