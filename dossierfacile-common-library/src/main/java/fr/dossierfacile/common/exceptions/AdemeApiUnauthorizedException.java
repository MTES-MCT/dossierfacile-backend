package fr.dossierfacile.common.exceptions;

public class AdemeApiUnauthorizedException extends Exception {
    public AdemeApiUnauthorizedException(int statusCode, String message) {
        super("ADEME API Unauthorized Error: status code " + statusCode + ", message " + message);
    }
}
