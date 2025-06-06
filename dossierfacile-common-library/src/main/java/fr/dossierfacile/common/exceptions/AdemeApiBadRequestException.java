package fr.dossierfacile.common.exceptions;

public class AdemeApiBadRequestException extends RuntimeException {
    public AdemeApiBadRequestException(String message) {
        super("ADEME API Bad Request Error: " + message);
    }
}
