package fr.dossierfacile.api.dossierfacileapiowner.register;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DPENotFoundException extends RuntimeException {
    public DPENotFoundException(String dpe) {
        super("Could not find DPE : " + dpe);
    }

    public DPENotFoundException() {
        super("Could not find DPE");
    }

}
