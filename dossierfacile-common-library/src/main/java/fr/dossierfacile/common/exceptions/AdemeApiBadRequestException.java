package fr.dossierfacile.common.exceptions;

public class AdemeApiBadRequestException extends Exception {
    public AdemeApiBadRequestException(String message) {
        super("ADEME API Bad Request Error: " + message);
    }
}
